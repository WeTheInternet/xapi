package xapi.fu.has;

import xapi.fu.Do;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/19/18 @ 1:18 AM.
 */
public interface HasPreload {
    void preload();

    static Do withPreload(Do task, HasPreload preload) {
        class PreloadDo implements Do, HasPreload {
            Do load;
            {
                load = ()->{
                    synchronized (PreloadDo.class) {
                        if (load == Do.NOTHING) {
                            return;
                        }
                        load = Do.NOTHING;
                    }
                    if (preload != null) {
                        preload.preload();
                    }
                    if (task instanceof HasPreload) {
                        ((HasPreload) task).preload();
                    }
                };
            }
            @Override
            public void done() {
                task.done();
            }

            @Override
            public void preload() {
                load.done();
            }
        }
        return new PreloadDo();
    }
}
