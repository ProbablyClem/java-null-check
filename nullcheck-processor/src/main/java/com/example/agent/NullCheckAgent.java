package com.example.agent;

import com.example.Assert;
import com.example.annotations.NullCheck;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.security.ProtectionDomain;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Java Agent that uses ByteBuddy to automatically inject null checks
 * into constructors of classes annotated with @NullCheck.
 *
 * Usage: java -javaagent:path/to/nullcheck-processor.jar -jar yourapp.jar
 */
public class NullCheckAgent {

    /**
     * Agent entry point called when the agent is loaded via -javaagent.
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[NullCheck Agent] Starting bytecode instrumentation...");

        new AgentBuilder.Default()
            // Only instrument classes annotated with @NullCheck
            .type(isAnnotatedWith(NullCheck.class))
            .transform(new AgentBuilder.Transformer() {
                @Override
                public DynamicType.Builder<?> transform(
                        DynamicType.Builder<?> builder,
                        TypeDescription typeDescription,
                        ClassLoader classLoader,
                        JavaModule module,
                        ProtectionDomain protectionDomain) {

                    System.out.println("[NullCheck Agent] Instrumenting class: " + typeDescription.getName());

                    // Inject null checks into all constructors using Advice
                    return builder.visit(Advice.to(ConstructorAdvice.class).on(isConstructor()));
                }
            })
            .with(AgentBuilder.Listener.StreamWriting.toSystemOut())
            .installOn(inst);

        System.out.println("[NullCheck Agent] Instrumentation complete!");
    }

    /**
     * Agent entry point for dynamic attachment.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }

    /**
     * ByteBuddy Advice class that injects null checks at the beginning of constructors.
     */
    public static class ConstructorAdvice {

        @Advice.OnMethodEnter
        public static void enter(
                @Advice.Origin Constructor<?> constructor,
                @Advice.AllArguments Object[] args) {

            // Get the class name
            String className = constructor.getDeclaringClass().getSimpleName();

            // Get parameter names
            Parameter[] parameters = constructor.getParameters();

            // Inject null check for each parameter
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                Object value = args[i];

                // Only check reference types (not primitives)
                if (!param.getType().isPrimitive()) {
                    String fieldName = className + "." + param.getName();
                    Assert.notNull(fieldName, value);
                }
            }
        }
    }
}
