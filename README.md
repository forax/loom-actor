# Loom Actor
A small but fun actor framework based on loom

This requires the latest loom early access build
[https://jdk.java.net/loom/](https://jdk.java.net/loom/).

## How to use the API

The class `Actor` defines an actor like an actor in Erlang or Akka.
An actor uses a virtual thread (also called coroutine) to run, it consumes messages from dedicated message queue
and can post messages to other actors.

Unlike a traditional actor system, the messages are lambdas that do a method call so the actor API is
a class (or an enum, record or even a lambda).

Moreover, the Actor API defines 3 different contexts
- the startup context, inside the consumer of `Actor.run(actor, code)`, the actions available are
  `postTo(actor, message)` to post a message to an actor and `spawn(actor)` to spawn a new actor instance.
- the actor context, inside the `actor.behavior(factory)` of an actor, the actions available are
  `currentActor(behaviorType)` to get the current actor, `panic(exception)` to report an exception,
   `postTo(actor, message)` to post a message to an actor, `spawn(actor)`to spawn a new actor instance and
  `shutdown()` to shutdown the current actor.
- the handler context, inside the `àctor.onSignal(signalHandler)`, the actions available are
  `postTo(actor, message)` to post a message to an actor, `restart()` to restart an actor whith a fresh behavior
  and `signal(actor, signal)` to send a signal message (an exception or a shutdown) to another actor.

Here is a simple example, `Actor.of()` create an actor, `behavior(factory)` defines the behavior, all the public
methods are message entry points. `Actors.run(actors, code)` spawn all the actors, run the code and
wait until all the actors are shutdown. In `Actor.run`, we post two messages to the actor, in response to the second
message "end", the actor shutdown itself, unblocking the method `Actor.run`.

```java
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
```

Another example is an HTTP server using an actor to implement a REST API using JSON.
The request body and the response body are JSON message that are automatically converted using Jackson.
The annotations @RequestMapping, @RequestBody and @PathVariable works like their Spring Web counterparts.
The last parameter is the actor that should receive a message corresponding to the HTTP response.

[todo.html](todo.html) is the corresponding web application written in vanilla JavaScript.
To see the application, run the main and use a browser to visit `http://localhost:8080/todo.html`.

```java
public class HttpMain {
  public static void main(String[] args) throws IOException, InterruptedException {
    record Task(long id, String content) {}
    record TaskWithoutId(String content) {}

    record TaskController(Context context, List<Task> tasks) {
      public static TaskController behavior(Context context) {
        var tasks = new ArrayList<Task>();
        // initial task
        tasks.add(new Task(0, "Hello from an http server powered by loom !"));
        return new TaskController(context, tasks);
      }

      @RequestMapping(path = "/tasks")
      public void getAllTasks(Actor<Response<List<Task>>> reply) {
        System.err.println("getAllTasks");
        context.postTo(reply, $ -> $.response(OK, tasks));
      }

      @RequestMapping(path = "/tasks", method = POST)
      public void createTask(@RequestBody TaskWithoutId taskWithoutId, Actor<Response<Task>> reply) {
        System.err.println("createTask " + taskWithoutId);
        var task = new Task(tasks.size(), taskWithoutId.content);
        tasks.add(task);
        context.postTo(reply, $ -> $.response(OK, task));
      }

      @RequestMapping(path = "/tasks/{id}", method = DELETE)
      public void deleteTask(@PathVariable("id") String id, Actor<Response<Void>> reply) {
        System.err.println("deleteTask " + id);
        var taskId = Integer.parseInt(id);
        var removed = tasks.removeIf(task -> task.id == taskId);
        if (!removed) {
          context.postTo(reply, $ -> $.response(NOT_FOUND, null));
          return;
        }
        context.postTo(reply, $ -> $.response(OK, null));
      }
    }

    var actor = Actor.of(TaskController.class)
        .behavior(TaskController::behavior)
        .onSignal((signal, context) -> {
          context.restart();  // restart if an error occurs
        });

    new HttpServer()
        .routes(actor)
        .bind(new InetSocketAddress("localhost", 8080));
  }
}
```

There are more examples in the folder [examples](src/main/examples/com/github/forax/loom/actor/examples).

## How to build

Download the latest early access build of loom [http://jdk.java.net/loom](http://jdk.java.net/loom)
set the environment variable JAVA_HOME to point to that JDK and then use Maven.

```
  export JAVA_HOME=/path/to/jdk
  mvn package
```
