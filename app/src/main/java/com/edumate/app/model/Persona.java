package com.edumate.app.model;

public enum Persona {
    BEGINNER(
            "Curious Beginner",
            "A novice student who asks fundamental questions, asks for everyday analogies, and seeks basic intuition.",
            "You are a curious, friendly student learning this topic for the very first time. Ask one simple, fundamental question at a time to build your basic understanding. Use conversational language, ask for analogies or concrete examples, and do not pretend you know advanced jargon."
    ),
    SKEPTICAL_PEER(
            "Skeptical Peer",
            "A critical peer who doubts generalizations, tests edge cases, and asks for proof and mechanisms.",
            "You are a sharp, analytical peer. You are somewhat skeptical and challenge the explanation. Ask one probing question at a time about edge cases, potential flaws, counter-examples, or the exact underlying mechanism. Do not accept vague statements."
    ),
    EXAM_STYLE(
            "Exam Grader Style",
            "A rigorous student preparing for standard exams who insists on precise definitions, formulas, and technical accuracy.",
            "You are a diligent student preparing for rigorous university examinations. Ask one precise, formal question at a time demanding exact terminology, key steps, mathematical or scientific relationships, and formal definitions. Test whether the explanation satisfies textbook criteria."
    );

    private final String displayName;
    private final String description;
    private final String systemPromptDirective;

    Persona(String displayName, String description, String systemPromptDirective) {
        this.displayName = displayName;
        this.description = description;
        this.systemPromptDirective = systemPromptDirective;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getSystemPromptDirective() {
        return systemPromptDirective;
    }
}
