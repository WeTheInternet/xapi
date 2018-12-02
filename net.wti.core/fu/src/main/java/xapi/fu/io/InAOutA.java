package xapi.fu.io;

import xapi.fu.In1Out1;

/**
 * A unary operator.
 *
 * We aren't using "unary, binary, trinary" naming,
 * because we want to generate patterns of mapped classes,
 * so the use of letters in the "standard In*Out* naming convention"
 * helps keep track of what a given named operation is doing.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/16/18 @ 4:36 AM.
 */
public interface InAOutA <I1> extends In1Out1<I1, I1> {
}
