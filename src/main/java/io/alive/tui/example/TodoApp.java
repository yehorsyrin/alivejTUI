package io.alive.tui.example;

import io.alive.tui.core.AliveJTUI;

/**
 * Entry point for the AliveJTUI Todo List example.
 *
 * <p>Run with:
 * <pre>
 *   mvn compile exec:java
 * </pre>
 *
 * Controls: ↑↓ navigate, Enter add item, Backspace delete char, ESC quit.
 *
 * @author Jarvis (AI)
 */
public class TodoApp {

    public static void main(String[] args) {
        AliveJTUI.run(new Showcase());
    }
}
