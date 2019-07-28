package xapi.process.api;

import xapi.collect.impl.SimpleStack;
import xapi.fu.In2;
import xapi.fu.In2Out1;
import xapi.scope.api.Scope;
import xapi.time.api.Moment;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/5/16.
 */
public interface ProcessConfiguration {
    enum Asynchronicity {
        IMMEDIATE, FINAL, DEFERRED, EVENTUALLY
    }

    enum RunContext {
        ANYWHERE, ON_MAIN, ON_UI, ON_EXECUTOR, ON_FORK_JOIN
    }

    enum ErrorResponse {
        IGNORE, RETHROW
    }

    Asynchronicity async();

    RunContext context();

    Iterable<In2<Scope, Boolean>> doneHandlers();

    Iterable<In2Out1<Scope, Throwable, ErrorResponse>> errorHandlers();

    Moment startAfter();




    interface ProcessConfigurationFactory {
        ProcessConfiguration build(

            Moment startAfter, Asynchronicity asynchronicity,
            RunContext runContext,
            SimpleStack<In2Out1<Scope, Throwable, ErrorResponse>> errorHandlers,
            SimpleStack<In2<Scope, Boolean>> doneHandlers
        );
    }

    static ProcessConfigurationBuilder builder() {
        return new ProcessConfigurationBuilder();
    }

    class ProcessConfigurationBuilder {
        private Asynchronicity asynchronicity = Asynchronicity.IMMEDIATE;
        private RunContext runContext = RunContext.ANYWHERE;
        private SimpleStack<In2Out1<Scope, Throwable, ErrorResponse>> errorHandlers = new SimpleStack<>();
        private SimpleStack<In2<Scope, Boolean>> doneHandlers = new SimpleStack<>();
        private ProcessConfigurationFactory configurationFactory;
        private Moment startAfter;

        public ProcessConfigurationBuilder setAsynchronicity(Asynchronicity async) {
            this.asynchronicity = async;
            return this;
        }

        public ProcessConfigurationBuilder setRunContext(RunContext context) {
            this.runContext = context;
            return this;
        }

        public ProcessConfigurationBuilder addDoneHandler(In2<Scope, Boolean> handler) {
            this.doneHandlers.add(handler);
            return this;
        }

        public ProcessConfigurationBuilder addErrorHandler(In2Out1<Scope, Throwable, ErrorResponse> handler) {
            this.errorHandlers.add(handler);
            return this;
        }

        public ProcessConfigurationBuilder setConfigurationFactory(ProcessConfigurationFactory factory) {
            this.configurationFactory = factory;
            return this;
        }

        public ProcessConfiguration build() {
            if (configurationFactory == null) {
                return new ProcessConfigurationDefault(
                    startAfter, asynchronicity, runContext, errorHandlers, doneHandlers);
            } else {
                return configurationFactory.build(startAfter, asynchronicity, runContext, errorHandlers, doneHandlers);
            }
        }

        public ProcessConfigurationBuilder setStartAfter(Moment startAfter) {
            this.startAfter = startAfter;
            return this;
        }
    }

    class ProcessConfigurationDefault implements ProcessConfiguration {

        private final Asynchronicity asynchronicity;
        private final RunContext runContext;
        private final SimpleStack<In2Out1<Scope, Throwable, ErrorResponse>> errorHandlers;
        private final SimpleStack<In2<Scope, Boolean>> doneHandlers;
        private final Moment startAfter;

        public ProcessConfigurationDefault(
            Moment startAfter, Asynchronicity asynchronicity,
            RunContext runContext,
            SimpleStack<In2Out1<Scope, Throwable, ErrorResponse>> errorHandlers,
            SimpleStack<In2<Scope, Boolean>> doneHandlers
        ) {
            this.startAfter = startAfter;
            this.asynchronicity = asynchronicity;
            this.runContext = runContext;
            this.errorHandlers = errorHandlers;
            this.doneHandlers = doneHandlers;
        }

        @Override
        public Asynchronicity async() {
            return asynchronicity;
        }

        @Override
        public RunContext context() {
            return runContext;
        }

        @Override
        public Iterable<In2Out1<Scope, Throwable, ErrorResponse>> errorHandlers() {
            return errorHandlers;
        }

        @Override
        public Moment startAfter() {
            return startAfter;
        }

        @Override
        public Iterable<In2<Scope, Boolean>> doneHandlers() {
            return doneHandlers;
        }
    }

}
