package net.wti.lang.parser.visitor;

import net.wti.lang.parser.ast.expr.AnnotationExpr
import net.wti.lang.parser.ast.expr.DynamicDeclarationExpr
import net.wti.lang.parser.ast.expr.IntegerLiteralExpr
import net.wti.lang.parser.ast.expr.JsonPairExpr
import net.wti.lang.parser.ast.expr.MemberValuePair
import net.wti.lang.parser.ast.expr.NameExpr
import net.wti.lang.parser.ast.expr.StringLiteralExpr
import net.wti.lang.parser.ast.expr.UiAttrExpr
import net.wti.lang.parser.ast.visitor.CloneVisitor
import net.wti.lang.parser.ast.body.MethodDeclaration
import spock.lang.Specification


///
/// CloneVisitorSpec:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/02/2026 @ 16:38
class CloneVisitorAnnotationTest extends Specification {

    private static List makeAnnotations() {
        [
                AnnotationExpr.newMarkerAnnotation("A"),
                AnnotationExpr.newSingleMemberAnnotation("B", StringLiteralExpr.stringLiteral("v")),
                AnnotationExpr.newAnnotation(
                        "C",
                        new MemberValuePair("x", new IntegerLiteralExpr("1")),
                        new MemberValuePair("y", StringLiteralExpr.stringLiteral("z")),
                )
        ]
    }

    def "CloneVisitor clones annotations attached to JsonPairExpr"() {
        given:
        def pair = new JsonPairExpr("k", StringLiteralExpr.stringLiteral("v"))
        pair.setAnnotations(makeAnnotations())

        when:
        def cloned = (JsonPairExpr) pair.accept(new CloneVisitor(), null)

        then:
        cloned !== pair
        cloned.getAnnotations() != null
        cloned.getAnnotations().size() == 3
        cloned.getAnnotations()*.getNameString() == ["A", "B", "C"]
        cloned.getAnnotations()[0] !== pair.getAnnotations()[0]
        cloned.getAnnotations()[1] !== pair.getAnnotations()[1]
        cloned.getAnnotations()[2] !== pair.getAnnotations()[2]
        cloned.getAnnotations()[2].getMembers().size() == 2
        cloned.getAnnotations()[2].getMembers().first() !== pair.getAnnotations()[2].getMembers().first()
    }

    def "CloneVisitor clones annotations attached to UiAttrExpr"() {
        given:
        def attr = new UiAttrExpr(new NameExpr("per"), false, StringLiteralExpr.stringLiteral("x"))
        attr.setAnnotations(makeAnnotations())

        when:
        def cloned = (UiAttrExpr) attr.accept(new CloneVisitor(), null)

        then:
        cloned !== attr
        cloned.getAnnotations() != null
        cloned.getAnnotations().size() == 3
        cloned.getAnnotations()*.getNameString() == ["A", "B", "C"]
        cloned.getAnnotations()[1] !== attr.getAnnotations()[1]
    }

    def "CloneVisitor clones annotations attached to DynamicDeclarationExpr"() {
        given:
        def body = new MethodDeclaration()
        body.setName("m")

        def dyn = new DynamicDeclarationExpr(body, makeAnnotations())

        when:
        def cloned = (DynamicDeclarationExpr) dyn.accept(new CloneVisitor(), null)

        then:
        cloned !== dyn
        cloned.getAnnotations() != null
        cloned.getAnnotations().size() == 3
        cloned.getAnnotations()*.getNameString() == ["A", "B", "C"]
        cloned.getAnnotations()[2] !== dyn.getAnnotations()[2]
        cloned.getBody() !== dyn.getBody()
    }
}