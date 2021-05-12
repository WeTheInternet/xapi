package xapi.inject.api;

import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.except.NotConfiguredCorrectly;
import xapi.platform.AndroidPlatform;
import xapi.platform.DevPlatform;
import xapi.platform.GwtDevPlatform;
import xapi.platform.GwtPlatform;
import xapi.platform.JavaFxPlatform;
import xapi.platform.JrePlatform;
import xapi.platform.Platform;
import xapi.prop.X_Properties;
import xapi.util.X_Util;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/18/16.
 */
@JrePlatform
@GwtPlatform
@DevPlatform
@GwtDevPlatform
@AndroidPlatform
@JavaFxPlatform
public class PlatformChecker {

    private final Set<String> runtime;
    private boolean needsInject;
    private Map<String, Set<String>> knownPlatforms;
    private Map<Class<? extends Annotation>, Integer> platformScore;
    private Map<String, Class<? extends Annotation>> allPlatforms;

    public PlatformChecker() {
        runtime = new HashSet<>();
        allPlatforms = new HashMap<>();
        platformScore = new HashMap<>();
        for (String allowed : X_Properties.platform.out1().split(",")) {
            runtime.add(allowed);
        }
        knownPlatforms = getKnownPlatforms();
        for (String choice : runtime) {
            if (!knownPlatforms.containsKey(choice)) {
                needsInject = true;
                break;
            }
        }
        sort();
    }

    public boolean needsInject() {
        return needsInject;
    }

    public void addPlatform(ClassLoader loader, String name, Iterable<String> choices) {
        final Set<String> result = knownPlatforms.computeIfAbsent(name, ignored->new LinkedHashSet<>());
        for (String choice : choices) {
            result.add(choice);
            String type = knownPlatforms.computeIfAbsent(choice, ignored->{
                final Set<String> set = new LinkedHashSet<>();
                set.add(choice);
                return set;
            }).iterator().next();
            try {
                allPlatforms.put(choice, (Class<? extends Annotation>) loader.loadClass(type));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw X_Util.rethrow(e);
            }
        }
        sort();
    }

    private void sort() {
        int score = 0;
        for (String current : runtime) {
            score++;
            for (String platform : knownPlatforms.get(current)) {
                final Class<? extends Annotation> type = allPlatforms.get(platform);
                platformScore.put(type, score);
            }
        }
        for (Class<? extends Annotation> platform : allPlatforms.values()) {
            platformScore.computeIfAbsent(platform, i->Integer.MIN_VALUE);
        }
    }

    public Class<?> findBest(Map<Class<?>, Integer> cls) {
        int best = Integer.MIN_VALUE;
        Class<?> result = null;
        for (Entry<Class<?>, Integer> entry : cls.entrySet()) {
            final Class<?> cl = entry.getKey();
            int score = entry.getValue() == null ? classScore(cl) : entry.getValue();
            if (score > best) {
                result = cl;
                best = score;
            }
        }
        return result;
    }

    public int classScore(Class<?> cl) {
        int score = Integer.MIN_VALUE;
        int bump = -1;
        for (Annotation annotation : cl.getAnnotations()) {
            if (annotation instanceof SingletonOverride) {
                score = ((SingletonOverride)annotation).priority();
            } else if (annotation instanceof InstanceOverride) {
                score = ((InstanceOverride)annotation).priority();
            } else if (annotation instanceof SingletonDefault || annotation instanceof InstanceDefault) {
                score = Integer.MIN_VALUE + 1;
            } else {
                Integer maybe = platformScore.get(annotation.annotationType());
                if (maybe != null) {
                    if (maybe != Integer.MIN_VALUE) {
                        if (maybe > bump) {
                            bump = maybe;
                        }
                    }
                }
            }
        }
        if (bump > -1) {
            score += bump;
        }
        return score;

    }

    public Map<String, Set<String>> getAllPlatforms() {
        return knownPlatforms;
    }

    protected Map<String, Set<String>> getKnownPlatforms() {
        final Annotation[] annos = getClass().getAnnotations();
        Map<String, Set<String>> results = new HashMap<>();
        for (Annotation anno : annos) {
            String type = anno.annotationType().getName();
            String simpleType = type.substring(type.lastIndexOf('.') + 1);
            final Set<String> into = results.computeIfAbsent(type, ignored->new LinkedHashSet());
            results.put(simpleType, into);
            into.add(type);
            collectFallbacks(into, anno.annotationType());
            allPlatforms.put(type, anno.annotationType());
            allPlatforms.put(simpleType, anno.annotationType());
        }
        return results;
    }

    private void collectFallbacks(Set<String> into, Class<? extends Annotation> cls) {
        Platform platform = cls.getAnnotation(Platform.class);
        if (platform == null) {
            throw new NotConfiguredCorrectly("Platform / fallback type " + cls + " did not have an @Platform annotation!");
        }
        for (Class<? extends Annotation> fallback : platform.fallback()) {
            if (into.add(fallback.getName())) {
                collectFallbacks(into, fallback);
            }
        }
    }

    public String[] getRuntime() {
        return runtime.toArray(new String[runtime.size()]);
    }
}
