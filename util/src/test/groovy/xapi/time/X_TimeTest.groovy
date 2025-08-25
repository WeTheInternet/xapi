package xapi.time

import spock.lang.Specification

class X_TimeTest extends Specification {

    def "X_Time.print can handle times greater than 1 day"() {
        when:
        long amount = 2 * X_Time.ONE_DAY + 3 * X_Time.ONE_HOUR + 30 * X_Time.ONE_MINUTE
        then:
        X_Time.print(amount) == "2D3h30m"
    }
}