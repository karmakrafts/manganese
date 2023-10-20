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

package io.karma.ferrous.manganese.compiler;

import io.karma.ferrous.manganese.analyze.Analyzer;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.translate.TranslationUnit;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import io.karma.kommons.function.Functions;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ListTokenSource;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;
import org.lwjgl.llvm.LLVMCore;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.llvm.LLVMCore.LLVMGetGlobalContext;

/**
 * @author Alexander Hinze
 * @since 02/07/2022
 */
@API(status = Status.STABLE)
public final class Compiler implements ANTLRErrorListener {
    private static final String[] IN_EXTENSIONS = {"ferrous", "fe"};
    private static final String OBJECT_FILE_EXTENSION = "o";

    private final TargetMachine targetMachine;

    private CompileContext context;
    private boolean tokenView = false;
    private boolean extendedTokenView = false;
    private boolean reportParserWarnings = false;
    private boolean disassemble = false;
    private boolean saveBitcode = false;
    private boolean isVerbose = false;

    @API(status = Status.INTERNAL)
    public Compiler(final TargetMachine targetMachine) {
        this.targetMachine = targetMachine;
    }

    @Override
    public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
                            final int charPositionInLine, final String msg, final RecognitionException e) {
        context.reportError(
                context.makeError((Token) offendingSymbol, Utils.makeCompilerMessage(Utils.capitalize(msg))),
                CompileStatus.SYNTAX_ERROR);
    }

    @Override
    public void reportAmbiguity(final Parser recognizer, final DFA dfa, final int startIndex, final int stopIndex,
                                final boolean exact, final BitSet ambigAlts, final ATNConfigSet configs) {
        if (reportParserWarnings) {
            Logger.INSTANCE.debugln("Detected ambiguity at %d:%d (%d)", startIndex, stopIndex, dfa.decision);
        }
    }

    @Override
    public void reportAttemptingFullContext(final Parser recognizer, final DFA dfa, final int startIndex,
                                            final int stopIndex, final BitSet conflictingAlts,
                                            final ATNConfigSet configs) {
        if (reportParserWarnings) {
            Logger.INSTANCE.debugln("Detected full context at %d:%d (%d)", startIndex, stopIndex, dfa.decision);
        }
    }

    @Override
    public void reportContextSensitivity(final Parser recognizer, final DFA dfa, final int startIndex,
                                         final int stopIndex, final int prediction, final ATNConfigSet configs) {
        if (reportParserWarnings) {
            Logger.INSTANCE.debugln("Detected abnormally high context sensitivity at %d:%d (%d)", startIndex, stopIndex,
                                    dfa.decision);
        }
    }

    public void analyze(final String name, final ReadableByteChannel in, final CompileContext context) {
        try {
            context.setModuleName(name);
            this.context = context; // Update context for every analysis

            final var charStream = CharStreams.fromChannel(in, StandardCharsets.UTF_8);

            tokenize(context, charStream);
            parse(context);
            if (!analyze(context)) {
                return;
            }
            process(context);
            context.setCurrentPass(CompilePass.NONE);
        }
        catch (Throwable error) {
            context.setCurrentPass(CompilePass.NONE);
            context.reportError(new CompileError(Utils.makeCompilerMessage(error.getMessage())),
                                CompileStatus.UNKNOWN_ERROR);
        }
    }

    public void compile(final String name, final String sourceName, final WritableByteChannel out,
                        final CompileContext context) {
        try {
            context.setModuleName(name);
            this.context = context; // Update context for every compilation

            if (!compile(context)) {
                return;
            }

            final var module = Objects.requireNonNull(context.getTranslationUnit()).getModule();
            module.setSourceFileName(sourceName);
            final var verificationStatus = module.verify();
            if (verificationStatus != null) {
                context.reportError(new CompileError(verificationStatus), CompileStatus.VERIFY_ERROR);
                return;
            }

            if (disassemble) {
                Logger.INSTANCE.infoln("");
                Logger.INSTANCE.info("%s", module.disassemble());
                Logger.INSTANCE.infoln("");
            }

            context.addModule(module);
            context.setCurrentPass(CompilePass.NONE);
        }
        catch (Exception error) {
            context.setCurrentPass(CompilePass.NONE);
            context.reportError(new CompileError(Utils.makeCompilerMessage(error.toString())),
                                CompileStatus.UNKNOWN_ERROR);
        }
    }

    public CompileResult compile(final Path in, final Path out, final Path buildDir, final CompileContext context) {
        if (!Files.exists(buildDir)) {
            try {
                Files.createDirectories(buildDir);
            }
            catch (Throwable error) {
                context.reportError(new CompileError(Utils.makeCompilerMessage(error.getMessage())),
                                    CompileStatus.IO_ERROR);
                return context.makeResult();
            }
        }

        final var inputFiles = Utils.findFilesWithExtensions(in, IN_EXTENSIONS);
        final var numFiles = inputFiles.size();
        final var maxProgress = numFiles << 1;

        for (var i = 0; i < numFiles; ++i) {
            final var file = inputFiles.get(i);
            // @formatter:off
            Logger.INSTANCE.infoln(Ansi.ansi()
                .fg(Color.GREEN)
                .a(Utils.getProgressIndicator(maxProgress, i))
                .a(Attribute.RESET)
                .a(" Analyzing file ")
                .fg(Color.BLUE)
                .a(Attribute.INTENSITY_BOLD)
                .a(file.toAbsolutePath().toString())
                .a(Attribute.RESET)
                .toString());
            // @formatter:on
            final var rawFileName = Utils.getRawFileName(file);
            Logger.INSTANCE.debugln("Input: %s", file);

            try (final var stream = Files.newInputStream(file); final var channel = Channels.newChannel(stream)) {
                analyze(rawFileName, channel, context);
            }
            catch (IOException error) {
                context.reportError(new CompileError(error.toString()), CompileStatus.IO_ERROR);
            }
            catch (Exception error) {
                context.reportError(new CompileError(error.toString()), CompileStatus.UNKNOWN_ERROR);
            }
        }

        final var moduleName = Utils.getRawFileName(in);
        final var module = targetMachine.createModule(moduleName);
        module.setSourceFileName(String.format("%s.%s", moduleName, targetMachine.getFileType().getExtension()));

        for (var i = 0; i < numFiles; ++i) {
            final var file = inputFiles.get(i);
            // @formatter:off
            Logger.INSTANCE.infoln(Ansi.ansi()
                .fg(Color.GREEN)
                .a(Utils.getProgressIndicator(maxProgress, numFiles + i))
                .a(Attribute.RESET)
                .a(" Compiling file ")
                .fg(Color.BLUE)
                .a(Attribute.INTENSITY_BOLD)
                .a(file.toAbsolutePath().toString())
                .a(Attribute.RESET)
                .toString());
            // @formatter:on
            final var rawFileName = Utils.getRawFileName(file);
            final var outFile = buildDir.resolve(String.format("%s.%s", rawFileName, OBJECT_FILE_EXTENSION));
            Logger.INSTANCE.debugln("Output: %s", outFile);

            try (final var stream = Files.newOutputStream(outFile); final var channel = Channels.newChannel(stream)) {
                compile(rawFileName, file.getFileName().toString(), channel, context);
                context.setCurrentPass(CompilePass.LINK);
                module.linkIn(context.getModule());
                context.setCurrentPass(CompilePass.NONE);
            }
            catch (IOException error) {
                context.reportError(new CompileError(error.toString()), CompileStatus.IO_ERROR);
            }
            catch (Exception error) {
                context.reportError(new CompileError(error.toString()), CompileStatus.UNKNOWN_ERROR);
            }
        }

        final var globalModule = Objects.requireNonNull(
                Functions.tryGet(() -> targetMachine.loadEmbeddedModule("global", LLVMGetGlobalContext())));
        Logger.INSTANCE.infoln("Global module:\n%s", globalModule.disassemble());
        module.linkIn(globalModule);
        globalModule.dispose();
        Logger.INSTANCE.infoln("Linked module:\n%s", module.disassemble());
        module.dispose();
        return context.makeResult();
    }

    public void setTokenView(final boolean tokenView, final boolean extendedTokenView) {
        this.tokenView = tokenView;
        this.extendedTokenView = extendedTokenView;
    }

    public void setSaveBitcode(boolean saveBitcode) {
        this.saveBitcode = saveBitcode;
    }

    public void setDisassemble(final boolean disassemble) {
        this.disassemble = disassemble;
    }

    public void setReportParserWarnings(final boolean reportParserWarnings) {
        this.reportParserWarnings = reportParserWarnings;
    }

    public boolean shouldDisassemble() {
        return disassemble;
    }

    public boolean isTokenViewEnabled() {
        return tokenView;
    }

    public boolean reportsParserWarnings() {
        return reportParserWarnings;
    }

    public boolean shouldSaveBitcode() {
        return saveBitcode;
    }

    public boolean isVerbose() {
        return isVerbose;
    }

    public void setVerbose(boolean verbose) {
        isVerbose = verbose;
    }

    public TargetMachine getTargetMachine() {
        return targetMachine;
    }

    @API(status = Status.INTERNAL)
    public CompileContext getContext() {
        return context;
    }

    private boolean checkStatus() {
        if (!context.getStatus().isRecoverable()) {
            Logger.INSTANCE.errorln("Compilation is irrecoverable, continuing to report syntax errors");
            return false;
        }
        return true;
    }

    private void processTokens(final List<Token> tokens) {
        // TODO: implement here
    }

    private void tokenize(final CompileContext context, final CharStream stream) {
        final var startTime = System.currentTimeMillis();
        context.setCurrentPass(CompilePass.TOKENIZE);
        final var lexer = new FerrousLexer(stream);
        context.setLexer(lexer);
        final var tokenStream = new CommonTokenStream(lexer);
        tokenStream.fill();
        context.setTokenStream(tokenStream);
        final var time = System.currentTimeMillis() - startTime;
        Logger.INSTANCE.debugln("Finished pass TOKENIZE in %dms", time);
        if (tokenView) {
            System.out.printf("\n%s\n", TokenUtils.renderTokenTree(context.getModuleName(), extendedTokenView, lexer,
                                                                   tokenStream.getTokens()));
        }
    }

    private void parse(final CompileContext context) {
        final var startTime = System.currentTimeMillis();
        context.setCurrentPass(CompilePass.PARSE);
        final var tokenStream = Objects.requireNonNull(context.getTokenStream());
        final var parser = new FerrousParser(tokenStream);
        parser.removeErrorListeners(); // Remove default error listener
        parser.addErrorListener(this);
        context.setParser(parser);
        context.setFileContext(parser.file());
        final var time = System.currentTimeMillis() - startTime;
        Logger.INSTANCE.debugln("Finished pass PARSE in %dms", time);
    }

    private boolean analyze(final CompileContext context) {
        final var startTime = System.currentTimeMillis();
        context.setCurrentPass(CompilePass.ANALYZE);
        final var analyzer = new Analyzer(this);
        context.setAnalyzer(analyzer);
        final var fileContext = Objects.requireNonNull(context.getFileContext());
        ParseTreeWalker.DEFAULT.walk(analyzer, fileContext);
        analyzer.preProcessTypes(); // Pre-materializes all UDTs in the right order
        final var time = System.currentTimeMillis() - startTime;
        Logger.INSTANCE.debugln("Finished pass ANALYZE in %dms", time);
        return checkStatus();
    }

    private void process(final CompileContext context) {
        final var tokenStream = Objects.requireNonNull(context.getTokenStream());
        final var tokens = tokenStream.getTokens();

        final var startTime = System.currentTimeMillis();
        context.setCurrentPass(CompilePass.PROCESS);
        processTokens(tokens);
        final var time = System.currentTimeMillis() - startTime;

        Logger.INSTANCE.debugln("Finished pass PROCESS in %dms", time);
        final var newTokenStream = new CommonTokenStream(new ListTokenSource(tokens));
        newTokenStream.fill();
        context.setTokenStream(newTokenStream);

        final var parser = Objects.requireNonNull(context.getParser());
        parser.setTokenStream(newTokenStream);
        parser.reset();
        parser.removeErrorListeners();
        parser.addErrorListener(this);
        context.setFileContext(parser.file());
    }

    private boolean compile(final CompileContext context) {
        final var startTime = System.currentTimeMillis();
        context.setCurrentPass(CompilePass.COMPILE);
        final var translationUnit = new TranslationUnit(this, context.getModuleName());
        ParseTreeWalker.DEFAULT.walk(translationUnit, context.getFileContext()); // Walk the entire AST with the TU
        context.setTranslationUnit(translationUnit);
        final var time = System.currentTimeMillis() - startTime;
        Logger.INSTANCE.debugln("Finished pass COMPILE in %dms", time);
        return checkStatus();
    }
}
