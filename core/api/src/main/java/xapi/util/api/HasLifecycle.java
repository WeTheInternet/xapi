package xapi.util.api;

public interface HasLifecycle {

  /**
   * Called once right after object is created.
   */
  void onInit();

  /**
   * @return true if startup is successful / should proceed, or false, if you want to fail startup.
   */
  boolean onStart();

  /**
   * @return true if shutdown is successful / should proceed, or false, if you want to cancel shutdown.
   */
  boolean onStop();

  /**
   * Called an object is destroyed (onStop returns true)
   */
  void onDestroy();

  class LifecycleHandler implements HasLifecycle {
    @Override
    public void onInit() {
    }

    @Override
    public boolean onStart() {
      return true;
    }

    @Override
    public boolean onStop() {
      return true;
    }

    @Override
    public void onDestroy() {
    }

  }

}
