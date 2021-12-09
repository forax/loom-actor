package com.github.forax.loom.actor.examples;

import com.github.forax.loom.actor.Actor;
import com.github.forax.loom.actor.Actor.Context;

import java.util.List;

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
