package net.wti.gradle.system.api;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.internal.ProjectViewInternal;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.system.impl.ImmutableTaskSpy;
import net.wti.gradle.system.service.GradleService;
import org.gradle.BuildAdapter;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionAdapter;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.TaskState;
import org.gradle.execution.taskgraph.TaskExecutionGraphInternal;

import java.util.WeakHashMap;

/**
 * TaskSpy:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 09/04/2021 @ 1:11 a.m..
 */
public interface TaskSpy {
    String EXT_NAME = "_xapiTS";

    MinimalProjectView getView();

    default <T extends Task> TaskSpy whenSelected(CharSequence path, Class<? extends T> cls, Action<? super T> callback) {

        return this;
    }

    /**
     *
     * @param view The project-path'd view who is requesting a TaskSpy interface.
     *
     *
     * @param path The key
     * @param cls
     * @param callback
     * @param <T>
     * @return
     */
    static <T extends Task> TaskSpy spy(ProjectViewInternal view, String path, Class<? extends T> cls, Action<? super T> callback) {
        final TaskSpy spy = GradleService.buildOnce(TaskWatcher.class, view.getRootProject(), EXT_NAME, onCreated -> {
            // code in here should only ever be called once (per however many times you send distinct ExtensionAware projectOrView as arguments)
            TaskWatcher watcher = new TaskWatcher();
            InitializeOnce once = new InitializeOnce(view, path, cls);
            watcher = watcher.viewOnce(view, once);
            return watcher;
        })
        .applySpy(view, path, cls, callback);

        return spy;
    }


}

final class TaskWatcher {

    private static final WeakHashMap<Gradle, TaskWatcher> bank = new WeakHashMap<>();

    protected TaskWatcher(){

    }

    protected <T extends Task> TaskSpy applySpy(final MinimalProjectView view, final String name, Class<? extends T> cls, final Action<? super T> callback) {
        final TaskSpy spy = new ImmutableTaskSpy(view);
        // Tell the spy about the task we want to watch
        watchTask(spy, name, cls, callback);
        return spy;
    }

    private <T extends Task> void watchTask(final TaskSpy spy, final String name, final Class<? extends T> cls, final Action<? super T> callback) {
        final MinimalProjectView view = spy.getView();

    }

    public TaskWatcher viewOnce(final ProjectViewInternal view, final InitializeOnce once) {
        final TaskWatcher result;
        final Gradle gradle = view.getGradle();
        synchronized (bank) {
            result = bank.computeIfAbsent(gradle, g -> {
                TaskWatcher watcher = new TaskWatcher();
                watcher.installMechanations(view);
                return watcher;
            });
        }

        return result;
    }

    private void installMechanations(final ProjectViewInternal view) {
        final Gradle gradle = view.getGradle();
        final TaskExecutionGraphInternal tg = (TaskExecutionGraphInternal) gradle.getTaskGraph();
        gradle.addBuildListener(new BuildAdapter() {
            @Override
            public void projectsLoaded(final Gradle gradle) {
                // can I touch the root project yet?
                System.out.println("Hello spy");
                System.out.println("Hello " + gradle.getRootProject());
            }

            @Override
            public void projectsEvaluated(final Gradle gradle) {
                tg.whenReady(teg -> {

                });
                tg.addTaskExecutionListener(new TaskExecutionAdapter() {
                    @Override
                    public void beforeExecute(final Task task) {
                        super.beforeExecute(task);
                    }

                    @Override
                    public void afterExecute(final Task task, final TaskState state) {
                        super.afterExecute(task, state);
                    }
                });
                tg.onNewTask(t -> {
                    // notify all whenConsidered + setup whenSelected
                });
                tg.addTaskExecutionGraphListener(taskExecutionGraph -> {

                });
            }

        });
    }
}

/**
 * A bag of debugging state, including a saved-up stack trace of when the spy was requested.
 */
class InitializeOnce {

    private static final class OnlyOnce extends Throwable {}

    private final MinimalProjectView initView;
    private final String initPath;
    private final Class<? extends Task> initCls;
    private final StackTraceElement[] invoker;

    <T extends Task> InitializeOnce(final MinimalProjectView view, final String path, final Class<? extends T> cls) {
        initView = view;
        initPath = path;
        initCls = cls;
        // expensive, but, by definition of our classname (InitialzeOnce), we should only be doing this ~once.
        invoker = new OnlyOnce().getStackTrace();
    }

}
