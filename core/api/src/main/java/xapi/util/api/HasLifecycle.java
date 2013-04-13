package xapi.util.api;

public interface HasLifecycle {

  void onInit();
  boolean onStart();
  boolean onStop();
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
      return false;
    }

    @Override
    public void onDestroy() {
    }

  }

}
