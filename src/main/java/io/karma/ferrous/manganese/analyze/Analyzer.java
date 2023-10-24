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

import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.Field;
import io.karma.ferrous.manganese.ocm.type.AliasedType;
import io.karma.ferrous.manganese.ocm.type.NamedType;
import io.karma.ferrous.manganese.ocm.type.NullType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.TypeAttribute;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.ocm.type.UDT;
import io.karma.ferrous.manganese.ocm.type.UDTKind;
import io.karma.ferrous.manganese.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.ScopeUtils;
import io.karma.ferrous.manganese.util.TypeUtils;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.AttribContext;
import io.karma.ferrous.vanadium.FerrousParser.ClassContext;
import io.karma.ferrous.vanadium.FerrousParser.EnumClassContext;
import io.karma.ferrous.vanadium.FerrousParser.IdentContext;
import io.karma.ferrous.vanadium.FerrousParser.StructContext;
import io.karma.ferrous.vanadium.FerrousParser.TraitContext;
import io.karma.ferrous.vanadium.FerrousParser.TypeAliasContext;
import io.karma.kommons.topo.TopoNode;
import io.karma.kommons.topo.TopoSorter;
import io.karma.kommons.tuple.Pair;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class Analyzer extends ParseAdapter {
    private final LinkedHashMap<Identifier, NamedType> udts = new LinkedHashMap<>();

    public Analyzer(Compiler compiler) {
        super(compiler);
    }

    private String addTypesToGraph(final ArrayDeque<Pair<TopoNode<NamedType>, TopoNode<NamedType>>> typesToResolve,
                                   final Map<Identifier, TopoNode<NamedType>> nodes,
                                   final HashSet<Identifier> resolved) {
        final var buffer = Ansi.ansi().fgBright(Color.CYAN);
        final var pair = typesToResolve.pop();

        final var parentNode = pair.getLeft();
        final var childNode = pair.getRight();
        final var childType = childNode.getValue();

        if (childType instanceof UDT udt && !udt.isComplete()) {
            buffer.fgBright(Color.MAGENTA).a('U').fgBright(Color.CYAN);
            final var fieldTypes = udt.type().getFieldTypes();
            for (final var fieldType : fieldTypes) {
                if (fieldType.isBuiltin() || fieldType.isComplete() || !(fieldType instanceof NamedType)) {
                    continue; // Skip all types which are complete to save time
                }
                var fieldTypeName = ((NamedType) fieldType).getQualifiedName();
                final var fieldNode = ScopeUtils.findInScope(nodes, fieldTypeName, childType.getScopeName());
                if (fieldNode == null) {
                    Logger.INSTANCE.errorln("Invalid field node '%s'", fieldTypeName);
                    continue;
                }
                fieldTypeName = fieldNode.getValue().getQualifiedName();
                if (resolved.contains(fieldTypeName)) {
                    buffer.fgBright(Color.GREEN).a('C').fgBright(Color.CYAN);
                    continue; // Prevent multiple passes over the same type
                }
                typesToResolve.push(Pair.of(childNode, fieldNode));
                resolved.add(fieldTypeName);
                buffer.a('R');
            }
        }

        if (childType instanceof AliasedType alias && !alias.isComplete()) {
            buffer.fgBright(Color.MAGENTA).a('A').fgBright(Color.CYAN);
            if (!alias.isBuiltin() && !alias.isComplete()) {
                var backingTypeName = ((NamedType) alias.getBackingType()).getQualifiedName();
                final var typeNode = ScopeUtils.findInScope(nodes, backingTypeName, childType.getScopeName());
                if (typeNode != null) {
                    backingTypeName = typeNode.getValue().getQualifiedName();
                    if (!resolved.contains(backingTypeName)) { // Prevent multiple passes over the same type
                        typesToResolve.push(Pair.of(childNode, typeNode));
                        resolved.add(backingTypeName);
                        buffer.a('R');
                    }
                    else {
                        buffer.fgBright(Color.GREEN).a('C').fgBright(Color.CYAN);
                    }
                }
            }
        }

        parentNode.addDependency(childNode);
        return buffer.a('D').fg(Color.CYAN).a(" > ").a(childType).toString();
    }

    @Override
    public void enterTypeAlias(final TypeAliasContext context) {
        final var identContext = context.ident();
        if (checkIsAlreadyDefined(identContext)) {
            return;
        }
        final var type = TypeUtils.getType(compiler, scopeStack, context.type());
        final var aliasedType = Types.aliased(Utils.getIdentifier(identContext), type,
                                              scopeStack::applyEnclosingScopes);
        udts.put(aliasedType.getQualifiedName(), aliasedType);
        super.enterTypeAlias(context);
    }

    @Override
    public void enterAttrib(AttribContext context) {
        final var identContext = context.ident();
        if (checkIsAlreadyDefined(identContext)) {
            return;
        }
        analyzeFieldLayout(context, Utils.getIdentifier(identContext), UDTKind.ATTRIBUTE);
        super.enterAttrib(context);
    }

    @Override
    public void enterStruct(final StructContext context) {
        final var identContext = context.ident();
        if (checkIsAlreadyDefined(identContext)) {
            return;
        }
        analyzeFieldLayout(context, Utils.getIdentifier(identContext), UDTKind.STRUCT);
        super.enterStruct(context);
    }

    @Override
    public void enterClass(final ClassContext context) {
        final var identContext = context.ident();
        if (checkIsAlreadyDefined(identContext)) {
            return;
        }
        analyzeFieldLayout(context, Utils.getIdentifier(identContext), UDTKind.CLASS);
        super.enterClass(context);
    }

    @Override
    public void enterEnumClass(final EnumClassContext context) {
        final var identContext = context.ident();
        if (checkIsAlreadyDefined(identContext)) {
            return;
        }
        analyzeFieldLayout(context, Utils.getIdentifier(identContext), UDTKind.ENUM_CLASS);
        super.enterEnumClass(context);
    }

    @Override
    public void enterTrait(final TraitContext context) {
        final var identContext = context.ident();
        if (checkIsAlreadyDefined(identContext)) {
            return;
        }
        analyzeFieldLayout(context, Utils.getIdentifier(identContext), UDTKind.TRAIT);
        super.enterTrait(context);
    }

    private boolean checkIsAlreadyDefined(final IdentContext identContext) {
        final var name = Utils.getIdentifier(identContext);
        final var type = findTypeInScope(name, scopeStack.getScopeName());
        if (type != null) {
            final var compileContext = compiler.getContext();
            final var message = Utils.makeCompilerMessage(String.format("Type '%s' is already defined", name));
            compileContext.reportError(compileContext.makeError(identContext.start, message, CompileErrorCode.E3000));
            return true;
        }
        return false;
    }

    public @Nullable Type findCompleteTypeInScope(final Identifier name, final Identifier scopeName) {
        Type type = ScopeUtils.findInScope(udts, name, scopeName);
        if (type == null) {
            return null;
        }
        while (type.isAliased()) {
            type = ((AliasedType) type).getBackingType();
        }
        if (!type.isComplete()) {
            if (!(type instanceof NamedType)) {
                return null;
            }
            final var typeName = ((NamedType) type).getQualifiedName();
            type = ScopeUtils.findInScope(udts, typeName, scopeName);
        }
        return type;
    }

    public @Nullable Type findTypeInScope(final Identifier name, final Identifier scopeName) {
        return ScopeUtils.findInScope(udts, name, scopeName);
    }

    private void resolveAliasedType(final AliasedType alias, final Identifier scopeName) {
        var currentType = alias.getBackingType();
        while (!currentType.isComplete()) {
            if (currentType.isAliased()) {
                currentType = ((AliasedType) currentType).getBackingType();
                continue;
            }
            if (!(currentType instanceof NamedType)) {
                Logger.INSTANCE.errorln("Could not resolve aliased type '%s'", alias.getQualifiedName());
                return;
            }
            final var currentTypeName = ((NamedType) currentType).getQualifiedName();
            final var completeType = findCompleteTypeInScope(currentTypeName, scopeName);
            if (completeType == null) {
                Logger.INSTANCE.errorln("Could not resolve aliased type '%s'", alias.getQualifiedName());
                return;
            }
            currentType = completeType;
        }
        alias.setBackingType(currentType);
    }

    private void resolveFieldTypes(final UDT udt, final Identifier scopeName) {
        final var type = udt.type();
        final var fieldTypes = type.getFieldTypes();
        final var numFields = fieldTypes.size();
        for (var i = 0; i < numFields; i++) {
            final var fieldType = fieldTypes.get(i);
            if (fieldType.isBuiltin() || fieldType.isComplete() || !(fieldType instanceof NamedType)) {
                continue;
            }
            final var fieldTypeName = ((NamedType) fieldType).getQualifiedName();
            Logger.INSTANCE.debugln("Found incomplete field type '%s' in '%s'", fieldTypeName, scopeName);
            final var completeType = findCompleteTypeInScope(fieldTypeName, scopeName);
            if (completeType == null) {
                Logger.INSTANCE.errorln("Failed to resolve field type '%s' for '%s'", fieldTypeName, scopeName);
                return;
            }
            Logger.INSTANCE.debugln("  > Resolved to complete type '%s'", completeType);
            type.setFieldType(i, completeType.derive(fieldType.getAttributes()));
        }
    }

    private void resolveTypes() {
        final var udts = this.udts.values();
        for (final var udt : udts) {
            final var scopeName = udt.getScopeName();
            if (udt.isAliased() && udt instanceof AliasedType alias) {
                resolveAliasedType(alias, scopeName);
            }
            if (!(udt instanceof UDT)) {
                continue; // Skip everything else apart from UDTs
            }
            resolveFieldTypes((UDT) udt, scopeName);
        }
    }

    private void materializeTypes() {
        for (final var udt : udts.values()) {
            if (udt.isAliased()) {
                continue; // Don't need to waste time on doing nothing..
            }
            if (!udt.isComplete()) {
                Logger.INSTANCE.errorln("Cannot materialize type '%s' as it is incomplete", udt.getQualifiedName());
                continue;
            }
            udt.materialize(compiler.getTargetMachine());
            Logger.INSTANCE.debugln("Materializing type %s", udt.getQualifiedName());
        }
    }

    private void sortTypes() {
        final var rootNode = new TopoNode<NamedType>(DummyType.INSTANCE);
        // @formatter:off
        final var nodes = udts.entrySet()
            .stream()
            .map(e -> Pair.of(e.getKey(), new TopoNode<>(e.getValue())))
            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        // @formatter:on

        final var queue = new ArrayDeque<Pair<TopoNode<NamedType>, TopoNode<NamedType>>>(); // <parent_node, child_node>
        final var resolved = new HashSet<Identifier>();
        for (final var nodeEntry : nodes.entrySet()) {
            final var node = nodeEntry.getValue();
            final var type = node.getValue();
            if (type == null) {
                continue; // Skip sorting
            }
            rootNode.getValue().setEnclosingScope(type.getEnclosingScope());
            queue.push(Pair.of(rootNode, node));
            while (!queue.isEmpty()) {
                Logger.INSTANCE.debugln("Resolving type graph: %s", addTypesToGraph(queue, nodes, resolved));
            }
            rootNode.addDependency(node);
        }

        Logger.INSTANCE.debugln("Reordering %d type entries", udts.size());
        final var sortedNodes = new TopoSorter<>(rootNode).sort(ArrayList::new);
        final var sortedMap = new LinkedHashMap<Identifier, NamedType>();

        for (final var node : sortedNodes) {
            final var type = node.getValue();
            if (type == null || type instanceof NullType || type == DummyType.INSTANCE) {
                continue;
            }
            final var qualifiedName = type.getQualifiedName();
            sortedMap.put(qualifiedName, udts.get(qualifiedName));
        }

        udts.clear();
        udts.putAll(sortedMap);
    }

    private void analyzeFieldLayout(final ParserRuleContext parent, final Identifier name, final UDTKind kind) {
        final var layoutAnalyzer = new FieldAnalyzer(compiler, scopeStack);
        ParseTreeWalker.DEFAULT.walk(layoutAnalyzer, parent);

        final var fieldTypes = layoutAnalyzer.getFields().stream().map(Field::getType).toArray(Type[]::new);

        final var type = Types.structure(name, scopeStack::applyEnclosingScopes, fieldTypes);
        final var udt = new UDT(kind, type);
        udts.put(type.getQualifiedName(), udt);
        Logger.INSTANCE.debugln("Captured field layout for type '%s'", type.getQualifiedName());
    }

    public void preProcessTypes() {
        sortTypes();
        resolveTypes();
        materializeTypes();
    }

    private static final class DummyType implements NamedType {
        public static final DummyType INSTANCE = new DummyType();
        private Scope enclosingScope;

        // @formatter:off
        private DummyType() {}
        // @formatter:on

        @Override
        public Identifier getName() {
            return Identifier.EMPTY;
        }

        @Override
        public long materialize(final TargetMachine machine) {
            throw new UnsupportedOperationException("Dummy type cannot be materialized");
        }

        @Override
        public TypeAttribute[] getAttributes() {
            return new TypeAttribute[0];
        }

        @Override
        public Type getBaseType() {
            return this;
        }

        @Override
        public @Nullable Scope getEnclosingScope() {
            return enclosingScope;
        }

        @Override
        public void setEnclosingScope(final Scope enclosingScope) {
            this.enclosingScope = enclosingScope;
        }
    }
}
