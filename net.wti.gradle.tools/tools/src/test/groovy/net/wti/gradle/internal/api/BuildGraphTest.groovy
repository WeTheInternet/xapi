package net.wti.gradle.internal.api

import net.wti.gradle.internal.require.api.BuildGraph
import net.wti.gradle.internal.require.impl.DefaultBuildGraph
import net.wti.gradle.system.service.GradleService
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.invocation.Gradle
import spock.lang.Specification
import spock.lang.Unroll
/**
 * Exercise the callback ordering of our build graph / worker queue algorithm,
 * to be sure it's acting the way we'd expect.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2/9/19 @ 4:26 AM.
 */
class BuildGraphTest extends Specification {

    @Unroll
    def "A simple build graph executes callbacks in order upTo #upTo"(int upTo) {
        given:
        Gradle g = Mock(Gradle)
        GradleService s = Mock(GradleService)
        ProjectView p = Mock(ProjectView) {
            1 * getDecorator() >> CollectionCallbackActionDecorator.NOOP
            2 * getGradle() >> g
        }
        BuildGraph b = new DefaultBuildGraph(s, p)
        def prev = Integer.MIN_VALUE

        when:
        Set<Integer> all = new HashSet<>()
        for (int i in ReadyState.all()) {
            if (i <= upTo) {
                all.add(i)
            }
            b.whenReady(i, {called->
                assert called > prev
                prev = called
                assert all.remove(called)
            })
        }

        then:
        !all.isEmpty()
        for (int i in ReadyState.all()) {
            if (i > upTo) {
                break
            }
            assert b.drainTasks(i)
        }
        all.isEmpty()

        where:
        upTo << [*ReadyState.all()] + Integer.MAX_VALUE
    }
}
