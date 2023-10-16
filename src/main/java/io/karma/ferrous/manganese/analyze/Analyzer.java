/*
 * Copyright 2023 Karma Krafts & associates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.karma.ferrous.manganese.analyze;

import io.karma.ferrous.manganese.Compiler;
import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.ocm.Field;
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.ocm.StructureType;
import io.karma.ferrous.manganese.ocm.Type;
import io.karma.ferrous.manganese.ocm.Types;
import io.karma.ferrous.manganese.ocm.UDT;
import io.karma.ferrous.manganese.ocm.UDTType;
import io.karma.ferrous.manganese.util.FunctionUtils;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.TypeUtils;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.AttribContext;
import io.karma.ferrous.vanadium.FerrousParser.AttributeListContext;
import io.karma.ferrous.vanadium.FerrousParser.ClassContext;
import io.karma.ferrous.vanadium.FerrousParser.EnumClassContext;
import io.karma.ferrous.vanadium.FerrousParser.IdentContext;
import io.karma.ferrous.vanadium.FerrousParser.InterfaceContext;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import io.karma.ferrous.vanadium.FerrousParser.StructContext;
import io.karma.ferrous.vanadium.FerrousParser.TraitContext;
import io.karma.kommons.topo.TopoNode;
import io.karma.kommons.topo.TopoSorter;
import io.karma.kommons.tuple.Pair;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Special translation unit ran during the pre-compilation pass
 * to discover declarations throughout the code.
 *
 * @author Alexander Hinze
 * @since 14/10/2023
 */
public final class Analyzer extends ParseAdapter {
    private final HashMap<Identifier, Function> functions = new HashMap<>();
    private final LinkedHashMap<Identifier, UDT> udts = new LinkedHashMap<>();

    public Analyzer(Compiler compiler) {
        super(compiler);
    }

    private boolean hasIncompleteTypes() {
        final var udts = this.udts.values();
        for (final var udt : udts) {
            final var type = udt.structureType();
            if (type == null || type.isComplete()) {
                continue;
            }
            return true;
        }
        return false;
    }

    public void preProcessTypes() {
        //final var udts = this.udts.values();
        //while (hasIncompleteTypes()) {
        //    for (final var udt : udts) {
        //        final var type = udt.structureType();
        //        if (type == null || type.isComplete()) {
        //            continue;
        //        }
        //
        //        final var fieldTypes = type.getFieldTypes();
        //        final var numFields = fieldTypes.size();
        //        for (var i = 0; i < numFields; i++) {
        //            final var fieldType = fieldTypes.get(i);
        //            final var completeUdt = this.udts.get(fieldType.getInternalName());
        //            if (completeUdt == null) {
        //                continue; // TODO: show warning/turn compilation irrecoverable?
        //            }
        //            final var completeType = completeUdt.structureType();
        //            if (completeType == null) {
        //                continue;
        //            }
        //            type.setFieldType(i, completeType);
        //        }
        //    }
        //}
        //
        //preMaterializeTypes();
    }

    private void addTypesToGraph(final Type type, final TopoNode<UDT> node,
                                 final Map<Identifier, TopoNode<UDT>> nodes) {
        if (type instanceof StructureType struct) {
            final var fieldTypes = struct.getFieldTypes();
            for (final var fieldType : fieldTypes) {
                addTypesToGraph(fieldType, node, nodes);
            }
            return;
        }
        final var typeNode = nodes.get(type.getInternalName());
        if (typeNode == null) {
            return;
        }
        node.addDependency(typeNode);
    }

    private void preMaterializeTypes() {
        final var rootNode = new TopoNode<>(UDT.NULL); // Dummy UDT; TODO: improve this
        // @formatter:off
        final var nodes = udts.entrySet()
            .stream()
            .map(e -> Pair.of(e.getKey(), new TopoNode<>(e.getValue())))
            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        // @formatter:on

        for (final var nodeEntry : nodes.entrySet()) {
            final var node = nodeEntry.getValue();
            final var type = node.getValue().structureType();
            if (type == null) {
                continue; // Skip sorting
            }
            addTypesToGraph(type, node, nodes);
            rootNode.addDependency(node);
        }

        Logger.INSTANCE.debugln("Reordering %d UDT entries", udts.size());
        final var sorter = new TopoSorter<>(rootNode);
        final var sortedNodes = sorter.sort(ArrayList::new);
        final var sortedMap = new LinkedHashMap<Identifier, UDT>();

        for (final var node : sortedNodes) {
            final var type = node.getValue().structureType();
            if (type == null) {
                continue;
            }
            Logger.INSTANCE.debugln("Pre-materializing type '%s'", type);
            type.materialize(compiler.getTarget()); // Pre-materialize
            final var internalName = type.getInternalName();
            sortedMap.put(internalName, udts.get(internalName));
        }

        udts.clear();
        udts.putAll(sortedMap);
    }

    private void analyzeFieldLayout(final ParserRuleContext parent, final IdentContext identContext,
                                    final UDTType udtType) {
        final var name = Utils.getIdentifier(identContext);

        final var parser = new FieldLayoutAnalyzer(compiler, scopeStack); // Copy scope stack
        ParseTreeWalker.DEFAULT.walk(parser, parent);

        final var fieldTypes = parser.getFields().stream().map(Field::type).toArray(Type[]::new);
        final var type = scopeStack.applyEnclosingScopes(Types.structure(name, fieldTypes));

        final var udt = new UDT(udtType, type);
        udts.put(type.getInternalName(), udt);
        Logger.INSTANCE.debugln("Captured field layout '%s'", udt);
    }

    private void analyzeAttributes(final @Nullable AttributeListContext context) {
        if (context == null) {
        }
        // TODO: ...
    }

    @Override
    public void enterStruct(StructContext context) {
        final var identifier = context.ident();
        analyzeFieldLayout(context, identifier, UDTType.STRUCT);
        analyzeAttributes(context.attributeList());
        super.enterStruct(context);
    }

    @Override
    public void enterClass(ClassContext context) {
        final var identifier = context.ident();
        analyzeFieldLayout(context, identifier, UDTType.CLASS);
        analyzeAttributes(context.attributeList());
        super.enterClass(context);
    }

    @Override
    public void enterEnumClass(EnumClassContext context) {
        final var identifier = context.ident();
        analyzeFieldLayout(context, identifier, UDTType.ENUM_CLASS);
        analyzeAttributes(context.attributeList());
        super.enterEnumClass(context);
    }

    @Override
    public void enterTrait(TraitContext context) {
        final var identifier = context.ident();
        analyzeFieldLayout(context, identifier, UDTType.TRAIT);
        analyzeAttributes(context.attributeList());
        super.enterTrait(context);
    }

    @Override
    public void enterInterface(InterfaceContext context) {
        analyzeAttributes(context.attributeList());
        super.enterInterface(context);
    }

    @Override
    public void enterAttrib(AttribContext context) {
        analyzeAttributes(context.attributeList());
        super.enterAttrib(context);
    }

    @Override
    public void enterProtoFunction(ProtoFunctionContext context) {
        final var name = FunctionUtils.getFunctionName(context.functionIdent());
        final var type = TypeUtils.getFunctionType(compiler, scopeStack, context);
        final var function = new Function(name, type);
        functions.put(name, function);
        Logger.INSTANCE.debugln("Found function '%s'", function);
        super.enterProtoFunction(context);
    }

    public HashMap<Identifier, Function> getFunctions() {
        return functions;
    }

    public LinkedHashMap<Identifier, UDT> getUDTs() {
        return udts;
    }
}