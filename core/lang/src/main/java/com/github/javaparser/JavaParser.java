/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2015 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */

package com.github.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.CommentsCollection;
import com.github.javaparser.ast.comments.CommentsParser;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import xapi.fu.In1Out1.In1Out1Unsafe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

// FIXME this file does not seem to be generated by javacc. Is the doc wrong, or the javacc config?
/**
 * <p>
 * This class was generated automatically by javacc, do not edit.
 * </p>
 * <p>
 * Parse Java 1.5 source code and creates Abstract Syntax Tree classes.
 * </p>
 *
 * @author Júlio Vilmar Gesser
 */
public final class JavaParser {
    private JavaParser() {
        // hide the constructor
    }

    private static boolean _doNotAssignCommentsPreceedingEmptyLines = true;

    private static boolean _doNotConsiderAnnotationsAsNodeStartForCodeAttribution = false;

    public static boolean getDoNotConsiderAnnotationsAsNodeStartForCodeAttribution()
    {
        return _doNotConsiderAnnotationsAsNodeStartForCodeAttribution;
    }

    public static void setDoNotConsiderAnnotationsAsNodeStartForCodeAttribution(boolean doNotConsiderAnnotationsAsNodeStartForCodeAttribution) {
        _doNotConsiderAnnotationsAsNodeStartForCodeAttribution = doNotConsiderAnnotationsAsNodeStartForCodeAttribution;
    }

    public static boolean getDoNotAssignCommentsPreceedingEmptyLines()
    {
        return _doNotAssignCommentsPreceedingEmptyLines;
    }

    public static void setDoNotAssignCommentsPreceedingEmptyLines(boolean doNotAssignCommentsPreceedingEmptyLines)
    {
        _doNotAssignCommentsPreceedingEmptyLines = doNotAssignCommentsPreceedingEmptyLines;
    }

    public static CompilationUnit parse(final InputStream in,
                                        final String encoding) throws ParseException {
        return parse(in,encoding,true);
    }

    public static UiContainerExpr parseXapi(final InputStream in,
                                        final String encoding) throws ParseException {
        return parseXapi(in,encoding,true);
    }

    /**
     * Parses the Java code contained in the {@link InputStream} and returns a
     * {@link CompilationUnit} that represents it.
     *
     * @param in
     *            {@link InputStream} containing Java source code
     * @param encoding
     *            encoding of the source code
     * @return CompilationUnit representing the Java source code
     * @throws ParseException
     *             if the source code has parser errors
     */
    public static CompilationUnit parse(final InputStream in,
                                        final String encoding, boolean considerComments) throws ParseException {
        return parse(in, encoding, considerComments, ASTParser::CompilationUnit);
    }

    public static UiContainerExpr parseXapi(final InputStream in,
                                        final String encoding, boolean considerComments) throws ParseException {
        return parse(in, encoding, considerComments, ASTParser::UiContainer);
    }

    public static <T extends Node> T parse(final InputStream in,
                                           final String encoding,
                                           boolean considerComments,
                                           In1Out1Unsafe<ASTParser, T> parseMethod) throws ParseException {
        try {
            // We are going to read in the full source input stream first,
            //
            String code = null;
            InputStream in1;
            if (considerComments) {
                code = SourcesHelper.streamToString(in, encoding);
                // since we already drained the input stream,
                // we need to rescope it onto this string (and allow real input to be released).
                in1 = SourcesHelper.stringToStream(code, encoding);
            } else {
                in1 = in;
            }
            final ASTParser parser = new ASTParser(in1, encoding);
            T cu = parseMethod.io(parser);

            if (considerComments){
                insertComments(cu,code);
            }
            return cu;
        } catch (IOException ioe){
            throw new ParseException(ioe.getMessage());
        }
    }

    /**
     * Parses the Java code contained in the {@link InputStream} and returns a
     * {@link CompilationUnit} that represents it.
     *
     * @param in
     *            {@link InputStream} containing Java source code
     * @return CompilationUnit representing the Java source code
     * @throws ParseException
     *             if the source code has parser errors
     */
    public static CompilationUnit parse(final InputStream in)
            throws ParseException {
        return parse(in, null,true);
    }

    public static CompilationUnit parse(final File file, final String encoding)
            throws ParseException, IOException {
        return parse(file,encoding,true);
    }

    public static UiContainerExpr parseXapi(final InputStream in)
            throws ParseException {
        return parseXapi(in, null,true);
    }

    public static UiContainerExpr parseXapi(final File file, final String encoding)
            throws ParseException, IOException {
        return parseXapi(file,encoding,true);
    }

    /**
     * Parses the Java code contained in a {@link File} and returns a
     * {@link CompilationUnit} that represents it.
     *
     * @param file
     *            {@link File} containing Java source code
     * @param encoding
     *            encoding of the source code
     * @return CompilationUnit representing the Java source code
     * @throws ParseException
     *             if the source code has parser errors
     * @throws IOException
     */
    public static CompilationUnit parse(final File file, final String encoding, boolean considerComments)
            throws ParseException, IOException {
        try(
            final FileInputStream in = new FileInputStream(file);
        ) {
            return parse(in, encoding, considerComments);
        }
    }

    public static UiContainerExpr parseXapi(final File file, final String encoding, boolean considerComments)
            throws ParseException, IOException {
        try(
            final FileInputStream in = new FileInputStream(file);
        ) {
            return parseXapi(in, encoding, considerComments);
        }
    }

    /**
     * Parses the Java code contained in a {@link File} and returns a
     * {@link CompilationUnit} that represents it.
     *
     * @param file
     *            {@link File} containing Java source code
     * @return CompilationUnit representing the Java source code
     * @throws ParseException
     *             if the source code has parser errors
     * @throws IOException
     */
    public static CompilationUnit parse(final File file) throws ParseException,
            IOException {
        return parse(file, null,true);
    }

    public static UiContainerExpr parseXapi(final File file) throws ParseException,
            IOException {
        return parseXapi(file, null,true);
    }

    public static CompilationUnit parse(final Reader reader, boolean considerComments)
            throws ParseException {
        return parseReader(reader, considerComments, ASTParser::CompilationUnit);
    }

    public static UiContainerExpr parseXapi(final Reader reader, boolean considerComments)
            throws ParseException {
        return parseReader(reader, considerComments, ASTParser::UiContainer);
    }

    public static <T extends Node> T parseReader(final Reader reader, boolean considerComments,
                                                 In1Out1Unsafe<ASTParser, T> parseMethod)
            throws ParseException {
        try {
            String code = SourcesHelper.readerToString(reader);
            Reader reader1 = SourcesHelper.stringToReader(code);
            final ASTParser parser = new ASTParser(reader1);
            T node = parseMethod.io(parser);
            if (considerComments){
                insertComments(node,code);
            }
            return node;
        } catch (IOException ioe){
            throw new ParseException(ioe.getMessage());
        }
    }

    /**
     * Parses the Java block contained in a {@link String} and returns a
     * {@link BlockStmt} that represents it.
     *
     * @param blockStatement
     *            {@link String} containing Java block code
     * @return BlockStmt representing the Java block
     * @throws ParseException
     *             if the source code has parser errors
     */
    public static BlockStmt parseBlock(final String blockStatement)
            throws ParseException {
        StringReader sr = new StringReader(blockStatement);
        BlockStmt result = new ASTParser(sr).Block();
        sr.close();
        return result;
    }

    public static Type parseType(final String type)
            throws ParseException {
        StringReader sr = new StringReader(type);
        Type result = new ASTParser(sr).Type();
        sr.close();
        return result;
    }

    /**
     * Parses the Java statement contained in a {@link String} and returns a
     * {@link Statement} that represents it.
     *
     * @param statement
     *            {@link String} containing Java statement code
     * @return Statement representing the Java statement
     * @throws ParseException
     *             if the source code has parser errors
     */
    public static Statement parseStatement(final String statement) throws ParseException {
        StringReader sr = new StringReader(statement);
        Statement stmt = new ASTParser(sr).Statement();
        sr.close();
        return stmt;
    }

    /**
     * Parses the Java import contained in a {@link String} and returns a
     * {@link ImportDeclaration} that represents it.
     *
     * @param importDeclaration
     *            {@link String} containing Java import code
     * @return ImportDeclaration representing the Java import declaration
     * @throws ParseException
     *             if the source code has parser errors
     */
    public static ImportDeclaration parseImport(final String importDeclaration) throws ParseException {
        StringReader sr = new StringReader(importDeclaration);
        ImportDeclaration id = new ASTParser(sr).ImportDeclaration();
        sr.close();
        return id;
    }

    /**
     * Parses the Java expression contained in a {@link String} and returns a
     * {@link Expression} that represents it.
     *
     * @param expression
     *            {@link String} containing Java expression
     * @return Expression representing the Java expression
     * @throws ParseException
     *             if the source code has parser errors
     */
    public static Expression parseExpression(final String expression) throws ParseException {
        StringReader sr = new StringReader(expression);
        Expression e = new ASTParser(sr).Expression();
        sr.close();
        return e;
    }

    public static UiContainerExpr parseUiContainer(final String uiContainer) throws ParseException {
        StringReader sr = new StringReader(uiContainer);
        final UiContainerExpr e = new ASTParser(sr).UiContainer();
        sr.close();
        return e;
    }

    public static Node parseNode(final String uiContainer) throws ParseException {
        if (uiContainer.trim().charAt(0) == '<') {
            return parseUiContainer(uiContainer);
        }
        StringReader sr = new StringReader(uiContainer);
        final CompilationUnit e = new ASTParser(sr).CompilationUnit();
        sr.close();
        return e;
    }

    public static JsonContainerExpr parseJsonContainer(final String uiContainer) throws ParseException {
        StringReader sr = new StringReader(uiContainer);
        final JsonContainerExpr e = new ASTParser(sr).JsonContainer();
        sr.close();
        return e;
    }

    /**
     * Parses the Java annotation contained in a {@link String} and returns a
     * {@link AnnotationExpr} that represents it.
     *
     * @param annotation
     *            {@link String} containing Java annotation
     * @return AnnotationExpr representing the Java annotation
     * @throws ParseException
     *             if the source code has parser errors
     */
    public static AnnotationExpr parseAnnotation(final String annotation) throws ParseException {
        StringReader sr = new StringReader(annotation);
        AnnotationExpr ae = new ASTParser(sr).Annotation();
        sr.close();
        return ae;
    }

    /**
     * Parses the Java body declaration(e.g fields or methods) contained in a
     * {@link String} and returns a {@link BodyDeclaration} that represents it.
     *
     * @param body
     *            {@link String} containing Java body declaration
     * @return BodyDeclaration representing the Java annotation
     * @throws ParseException
     *             if the source code has parser errors
     */
    public static BodyDeclaration parseBodyDeclaration(final String body) throws ParseException {
        StringReader sr = new StringReader(body);
        BodyDeclaration bd = new ASTParser(sr).AnnotationBodyDeclaration();
        sr.close();
        return bd;
    }

    /**
     * Comments are attributed to the thing the comment and are removed from
     * allComments.
     */
    private static void insertCommentsInCu(CompilationUnit cu, CommentsCollection commentsCollection){
        if (commentsCollection.size()==0) return;

        // I should sort all the direct children and the comments, if a comment is the first thing then it
        // a comment to the CompilationUnit
        // FIXME if there is no package it could be also a comment to the following class...
        // so I could use some heuristics in these cases to distinguish the two cases

        List<Comment> comments = commentsCollection.getAll();
        PositionUtils.sortByBeginPosition(comments);
        List<Node> children = cu.getChildrenNodes();
        PositionUtils.sortByBeginPosition(children);

        if (cu.getPackage()!=null && (children.isEmpty() || PositionUtils.areInOrder(comments.get(0), children.get(0)))){
            cu.setComment(comments.get(0));
            comments.remove(0);
        }

        insertCommentsInNode(cu,comments);
    }

    private static boolean attributeLineCommentToNodeOrChild(Node node, LineComment lineComment)
    {
        // The node start and end at the same line as the comment,
        // let's give to it the comment
        if (node.getBeginLine()==lineComment.getBeginLine() && !node.hasComment())
        {
            node.setComment(lineComment);
            return true;
        } else {
            // try with all the children, sorted by reverse position (so the
            // first one is the nearest to the comment
            List<Node> children = new LinkedList<Node>();
            children.addAll(node.getChildrenNodes());
            PositionUtils.sortByBeginPosition(children);
            Collections.reverse(children);

            for (Node child : children)
            {
                if (attributeLineCommentToNodeOrChild(child, lineComment))
                {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * This method try to attributes the nodes received to child of the node.
     * It returns the node that were not attributed.
     */
    private static void insertCommentsInNode(Node node, List<Comment> commentsToAttribute){
        if (commentsToAttribute.isEmpty()) return;

        // the comments can:
        // 1) Inside one of the child, then it is the child that have to associate them
        // 2) If they are not inside a child they could be preceeding nothing, a comment or a child
        //    if they preceed a child they are assigned to it, otherweise they remain "orphans"

        List<Node> children = node.getChildrenNodes();
        PositionUtils.sortByBeginPosition(children);

        for (Node child : children){
            List<Comment> commentsInsideChild = new LinkedList<Comment>();
            for (Comment c : commentsToAttribute){
                if (PositionUtils.nodeContains(child, c, _doNotConsiderAnnotationsAsNodeStartForCodeAttribution)){
                    commentsInsideChild.add(c);
                }
            }
            commentsToAttribute.removeAll(commentsInsideChild);
            insertCommentsInNode(child,commentsInsideChild);
        }

        // I can attribute in line comments to elements preceeding them, if there
        // is something contained in their line
        List<Comment> attributedComments = new LinkedList<Comment>();
        for (Comment comment : commentsToAttribute)
        {
            if (comment.isLineComment())
            {
                for (Node child : children)
                {
                    if (child.getEndLine()==comment.getBeginLine())
                    {
                        if (attributeLineCommentToNodeOrChild(child, comment.asLineComment()))
                        {
                            attributedComments.add(comment);
                        }
                    }
                }
            }
        }

        // at this point I create an ordered list of all remaining comments and children
        Comment previousComment = null;
        attributedComments = new LinkedList<Comment>();
        List<Node> childrenAndComments = new LinkedList<Node>();
        childrenAndComments.addAll(children);
        childrenAndComments.addAll(commentsToAttribute);
        PositionUtils.sortByBeginPosition(childrenAndComments, _doNotConsiderAnnotationsAsNodeStartForCodeAttribution);

        for (Node thing : childrenAndComments){
            if (thing instanceof Comment){
                previousComment = (Comment)thing;
                if (!previousComment.isOrphan())
                {
                    previousComment = null;
                }
            } else {
                if (previousComment != null && !thing.hasComment()){
                    if (!_doNotAssignCommentsPreceedingEmptyLines || !thereAreLinesBetween(previousComment, thing)) {
                        thing.setComment(previousComment);
                        attributedComments.add(previousComment);
                        previousComment = null;
                    }
                }
            }
        }

        commentsToAttribute.removeAll(attributedComments);

        // all the remaining are orphan nodes
        for (Comment c : commentsToAttribute){
            if (c.isOrphan()) {
                node.addOrphanComment(c);
            }
        }
    }

    private static boolean thereAreLinesBetween(Node a, Node b)
    {
        if (!PositionUtils.areInOrder(a, b))
        {
            return thereAreLinesBetween(b, a);
        }
        int endOfA = a.getEndLine();
        return b.getBeginLine()>(a.getEndLine()+1);
    }

    private static void insertComments(Node cu, String code) throws IOException {
        CommentsParser commentsParser = new CommentsParser();
        CommentsCollection allComments = commentsParser.parse(code);

        if (cu instanceof CompilationUnit) {
            insertCommentsInCu((CompilationUnit) cu,allComments);
        } else {
            List<Comment> comments = allComments.getAll();
            PositionUtils.sortByBeginPosition(comments);

            insertCommentsInNode(cu, comments);
        }
    }

}
