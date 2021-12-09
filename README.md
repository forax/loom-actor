# loom-actor
A small but fun actor framework based on loom



````java
public class HelloMain {
  public static void main(String[] args) throws InterruptedException {
    record Hello(Context context) {
      public void say(String message) {
        System.out.println("Hello " + message);
      }

      public void end() {
        context.shutdown();
      }
    }

    var hello = Actor.of(Hello.class);
    hello.behavior(Hello::new);

    Actor.run(List.of(hello), context -> {
      context.postTo(hello, $ -> $.say("actors using loom"));
      context.postTo(hello, $ -> $.end());
    });
  }
}
````
