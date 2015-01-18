/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * MODIFIED BY James Nelson of We The Internet, 2013.
 * Repackaged to avoid conflicts with different versions of Javassist,
 * and modified Javassist APIs to make them more accessible to outside code.
 */
package xapi.bytecode.api;

public enum ByteCodes implements IsByteCode {
/**
  arrayref, index → value load onto the stack a reference from an array
 */
  aaload(0x32, 2),
/**
  arrayref, index, value →  store into a reference in an array
*/
  aastore(0x53, 3),
/**
  → null  push a null reference onto the stack
*/
  aconst_null(1, 0),
/**
  1: index  → objectref load a reference onto the stack from a local variable #index
*/
  aload(0x19, 1),
/**
  → objectref load a reference onto the stack from local variable 0
*/
  aload_0(0x2a, 0),
/**
  → objectref load a reference onto the stack from local variable 1
 */
  aload_1(0x2b, 0),
/**
  → objectref load a reference onto the stack from local variable 2
*/
  aload_2(0x2c, 0),
/**
  → objectref load a reference onto the stack from local variable 3
*/
  aload_3(0x2d, 0),
/**
  2: indexbyte1, indexbyte2 count → arrayref  create a new array of references of length count and component type identified by the class reference index (indexbyte1 << 8 + indexbyte2) in the constant pool
*/
  anewarray(0xbd, 3),
/**
  objectref → [empty] return a reference from a method
*/
  areturn(0xb0, 1),
/**
  arrayref → length get the length of an array
*/
  arraylength(0xbe, 1),
/**
  1: index  objectref → store a reference into a local variable #index
*/
  astore(0x3a, 2),
/**
  objectref → store a reference into local variable 0
*/
  astore_0(0x4b, 1),
/**
  objectref → store a reference into local variable 1
*/
  astore_1(0x4c, 1),
/**
  objectref → store a reference into local variable 2
*/
  astore_2(0x4d, 1),
/**
  objectref → store a reference into local variable 3
*/
  astore_3(0x4e, 1),
/**
  objectref → [empty], objectref  throws an error or exception (notice that the rest of the stack is cleared, leaving only a reference to the Throwable)
*/
  athrow(0xbf, 1),
/**
  arrayref, index → value load a byte or Boolean value from an array
*/
  baload(0x33, 2),
/**
  arrayref, index, value →  store a byte or Boolean value into an array
*/
  bastore(0x54, 3),
/**
  10  1: byte → value push a byte onto the stack as an integer value
*/
  bipush(1, 1),
/**
  arrayref, index → value load a char from an array
*/
  caload(0x34, 2),
/**
  arrayref, index, value →  store a char into an array
*/
  castore(0x55, 2),
/**
  2: indexbyte1, indexbyte2 objectref → objectref checks whether an objectref is of a certain type, the class reference of which is in the constant pool at index (indexbyte1 << 8 + indexbyte2)
*/
  checkcast(0xc0, 3),
/**
  value1, value2 → result add two doubles
*/
  dadd(0x63, 2),
/**
  arrayref, index → value load a double from an array
*/
  daload(0x31, 2),
/**
  arrayref, index, value →  store a double into an array
*/
  dastore(0x52, 3),
/**
  value1, value2 → result compare two doubles
*/
  dcmpg(0x98, 2),
/**
  value1, value2 → result compare two doubles
*/
  dcmpl(0x97, 2),
/**
  → 0.0 push the constant 0.0 onto the stack
*/
  dconst_0(0x0e, 0),
/**
  → 1.0 push the constant 1.0 onto the stack
*/
  dconst_1(0x0f, 0),
/**
  value1, value2 → result divide two doubles
*/
  ddiv(0x6f, 2),
/**
  1: index  → value load a double value from a local variable #index
*/
  dload(0x18, 1),
/**
  → value load a double from local variable 0
*/
  dload_0(0x26, 0),
/**
  → value load a double from local variable 1
*/
  dload_1(0x27, 0),
/**
  → value load a double from local variable 2
*/
  dload_2(0x28, 0),
/**
  → value load a double from local variable 3
*/
  dload_3(0x29, 0),
/**
  value1, value2 → result multiply two doubles
*/
  dmul(0x6b, 2),
/**
  value → result  negate a double
*/
  dneg(0x77, 1),
/**
  value1, value2 → result get the remainder from a division between two doubles
*/
  drem(0x73, 2),
/**
  value → [empty] return a double from a method
*/
  dreturn(0xaf, 1),
/**
  1: index  value → store a double value into a local variable #index
*/
  dstore(0x39, 2),
/**
  value → store a double into local variable 0
*/
  dstore_0(0x47, 1),
/**
  value → store a double into local variable 1
*/
  dstore_1(0x48, 1),
/**
  value → store a double into local variable 2
*/
  dstore_2(0x49, 1),
/**
  value → store a double into local variable 3
*/
  dstore_3(0x4a, 1),
/**
  value1, value2 → result subtract a double from another
*/
  dsub(0x67, 2),
/**
  value → value, value  duplicate the value on top of the stack
*/
  dup(0x59, 1),
/**
  value2, value1 → value1, value2, value1 insert a copy of the top value into the stack two values from the top. value1 and value2 must not be of the type double or long.
*/
  dup_x1(0x5a, 2),
/**
  value3, value2, value1 → value1, value3, value2, value1 insert a copy of the top value into the stack two (if value2 is double or long it takes up the entry of value3, too) or three values (if value2 is neither double nor long) from the top
*/
  dup_x2(0x5b, 3),
/**
  {value2, value1} → {value2, value1}, {value2, value1} duplicate top two stack words (two values, if value1 is not double nor long; a single value, if value1 is double or long)
*/
  dup2(0x5c, 2),
/**
  value3, {value2, value1} → {value2, value1}, value3, {value2, value1} duplicate two words and insert beneath third word (see explanation above)
*/
  dup2_x1(0x5d, 3),
/**
  {value4, value3}, {value2, value1} → {value2, value1}, {value4, value3}, {value2, value1} duplicate two words and insert beneath fourth word
*/
  dup2_x2(0x5e, 4),
/**
  value1, value2 → result add two floats
*/
  fadd(0x62, 2),
/**
  arrayref, index → value load a float from an array
*/
  faload(0x30, 2),
/**
  arrayref, index, value →  store a float in an array
*/
  fastore(0x51, 3),
/**
  value1, value2 → result compare two floats
*/
  fcmpg(0x96, 2),
/**
  value1, value2 → result compare two floats
*/
  fcmpl(0x95, 2),
/**
  → 0.0f  push 0.0f on the stack
*/
  fconst_0(0x0b, 0),
/**
  → 1.0f  push 1.0f on the stack
*/
  fconst_1(0x0c, 0),
/**
  → 2.0f  push 2.0f on the stack
*/
  fconst_2(0x0d, 0),
/**
  value1, value2 → result divide two floats
*/
  fdiv(0x6e, 2),
/**
  1: index  → value load a float value from a local variable #index
*/
  fload(0x17, 1),
/**
  → value load a float value from local variable 0
*/
  fload_0(0x22, 0),
/**
  → value load a float value from local variable 1
*/
  fload_1(0x23, 0),
/**
  → value load a float value from local variable 2
*/
  fload_2(0x24, 0),
/**
  → value load a float value from local variable 3
*/
  fload_3(0x25, 0),
/**
  value1, value2 → result multiply two floats
*/
  fmul(0x6a, 2),
/**
  value → result  negate a float
*/
  fneg(0x76, 1),
/**
  value1, value2 → result get the remainder from a division between two floats
*/
  frem(0x72, 2),
/**
  value → [empty] return a float
*/
  freturn(0xae, 1),
/**
  1: index  value → store a float value into a local variable #index
*/
  fstore(0x38, 2),
/**
  value → store a float value into local variable 0
*/
  fstore_0(0x43, 1),
/**
  value → store a float value into local variable 1
*/
  fstore_1(0x44, 1),
/**
  value → store a float value into local variable 2
*/
  fstore_2(0x45, 1),
/**
  value → store a float value into local variable 3
*/
  fstore_3(0x46, 1),
/**
  value1, value2 → result subtract two floats
*/
  fsub(0x66, 2),
/**
  2: index1, index2 objectref → value get a field value of an object objectref, where the field is identified by field reference in the constant pool index (index1 << 8 + index2)
*/
  getfield(0xb4, 3),
/**
  2: index1, index2 → value get a static field value of a class, where the field is identified by field reference in the constant pool index (index1 << 8 + index2)
*/
  getstatic(0xb2, 2),
/**
  2: branchbyte1, branchbyte2 [no change] goes to another instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
 */
  _goto(0xa7, 2),
/**
  4: branchbyte1, branchbyte2, branchbyte3, branchbyte4 [no change] goes to another instruction at branchoffset (signed int constructed from unsigned bytes branchbyte1 << 24 + branchbyte2 << 16 + branchbyte3 << 8 + branchbyte4)
*/
  goto_w(0xc8, 4),
/**
  value → result  convert an int into a long
*/
  i2l(0x85, 1),
/**
  value → result  convert an int into a float
*/
  i2f(0x86, 1),
/**
  value → result  convert an int into a double
*/
  i2d(0x87, 1),
/**
  value → result  convert a long to a int
*/
  l2i(0x88, 1),
/**
  value → result  convert a long to a float
*/
  l2f(0x89, 1),
/**
  value → result  convert a long to a double
*/
  l2d(0x8a, 1),
/**
  value → result  convert a float to an int
*/
  f2i(0x8b, 1),
/**
  value → result  convert a float to a long
*/
  f2l(0x8c, 1),
/**
  value → result  convert a float to a double
*/
  f2d(0x8d, 1),
/**
  value → result  convert a double to an int
*/
  d2i(0x8e, 1),
/**
  value → result  convert a double to a long
*/
  d2l(0x8f, 1),
/**
  value → result  convert a double to a float
*/
  d2f(0x90, 1),
/**
  value → result  convert an int into a byte
*/
  i2b(0x91, 1),
/**
  value → result  convert an int into a character
*/
  i2c(0x92, 1),
/**
  value → result  convert an int into a short
*/
  i2s(0x93, 1),
/**
  value1, value2 → result add two ints
*/
  iadd(0x60, 2),
/**
  arrayref, index → value load an int from an array
*/
  iaload(0x2e, 2),
/**
  value1, value2 → result perform a bitwise and on two integers
*/
  iand(0x7e, 2),
/**
  arrayref, index, value →  store an int into an array
*/
  iastore(0x4f, 3),
/**
  → -1  load the int value -1 onto the stack
*/
  iconst_m1(0x02, 0),
/**
  → 0 load the int value 0 onto the stack
*/
  iconst_0(0x03, 0),
/**
  → 1 load the int value 1 onto the stack
*/
  iconst_1(0x04, 0),
/**
  → 2 load the int value 2 onto the stack
*/
  iconst_2(0x05, 0),
/**
  → 3 load the int value 3 onto the stack
*/
  iconst_3(0x06, 0),
/**
  → 4 load the int value 4 onto the stack
*/
  iconst_4(0x07, 0),
/**
  → 5 load the int value 5 onto the stack
*/
  iconst_5(0x08, 0),
/**
  value1, value2 → result divide two integers
*/
  idiv(0x6c, 2),
/**
  2: branchbyte1, branchbyte2 value1, value2 →  if references are equal, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  if_acmpeq(0xa5, 4),
/**
  2: branchbyte1, branchbyte2 value1, value2 →  if references are not equal, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  if_acmpne(0xa6, 4),
/**
  2: branchbyte1, branchbyte2 value1, value2 →  if ints are equal, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  if_icmpeq(0x9f, 4),
/**
  2: branchbyte1, branchbyte2 value1, value2 →  if ints are not equal, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  if_icmpne(0xa0, 4),
/**
  2: branchbyte1, branchbyte2 value1, value2 →  if value1 is less than value2, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  if_icmplt(0xa1, 4),
/**
  2: branchbyte1, branchbyte2 value1, value2 →  if value1 is greater than or equal to value2, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  if_icmpge(0xa2, 4),
/**
  2: branchbyte1, branchbyte2 value1, value2 →  if value1 is greater than value2, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  if_icmpgt(0xa3, 4),
/**
  2: branchbyte1, branchbyte2 value1, value2 →  if value1 is less than or equal to value2, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  if_icmple(0xa4, 4),
/**
  2: branchbyte1, branchbyte2 value → if value is 0, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  ifeq(0x99, 3),
/**
  2: branchbyte1, branchbyte2 value → if value is not 0, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  ifne(0x9a, 3),
/**
  2: branchbyte1, branchbyte2 value → if value is less than 0, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  iflt(0x9b, 3),
/**
  2: branchbyte1, branchbyte2 value → if value is greater than or equal to 0, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  ifge(0x9c, 3),
/**
  2: branchbyte1, branchbyte2 value → if value is greater than 0, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  ifgt(0x9d, 3),
/**
  2: branchbyte1, branchbyte2 value → if value is less than or equal to 0, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  ifle(0x9e, 3),
/**
  2: branchbyte1, branchbyte2 value → if value is not null, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  ifnonnull(0xc7, 3),
/**
  2: branchbyte1, branchbyte2 value → if value is null, branch to instruction at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2)
*/
  ifnull(0xc6, 3),
/**
  2: index, const [No change] increment local variable #index by signed byte const
*/
  iinc(0x84, 2),
/**
  1: index  → value load an int value from a local variable #index
*/
  iload(0x15, 1),
/**
  → value load an int value from local variable 0
*/
  iload_0(0x1a, 0),
/**
  → value load an int value from local variable 1
*/
  iload_1(0x1b, 0),
/**
  → value load an int value from local variable 2
*/
  iload_2(0x1c, 0),
/**
  → value load an int value from local variable 3
*/
  iload_3(0x1d, 0),
/**
  value1, value2 → result multiply two integers
*/
  imul(0x68, 2),
/**
  value → result  negate int
*/
  ineg(0x74, 1),
/**
  2: indexbyte1, indexbyte2 objectref → result  determines if an object objectref is of a given type, identified by class reference index in constant pool (indexbyte1 << 8 + indexbyte2)
*/
  _instanceof (0xc1, 3),
/**
  4: indexbyte1, indexbyte2, 0, 0 [arg1, [arg2 ...]] →  invokes a dynamic method identified by method reference index in constant pool (indexbyte1 << 8 + indexbyte2)
*/
  invokedynamic(0xba, -2),
/**
  4: indexbyte1, indexbyte2, count, 0 objectref, [arg1, arg2, ...] →  invokes an interface method on object objectref, where the interface method is identified by method reference index in constant pool (indexbyte1 << 8 + indexbyte2)
*/
  invokeinterface(0xb9, -5),
/**
  2: indexbyte1, indexbyte2 objectref, [arg1, arg2, ...] →  invoke instance method on object objectref, where the method is identified by method reference index in constant pool (indexbyte1 << 8 + indexbyte2)
*/
  invokespecial(0xb7, -3),
/**
  2: indexbyte1, indexbyte2 [arg1, arg2, ...] → invoke a static method, where the method is identified by method reference index in constant pool (indexbyte1 << 8 + indexbyte2)
*/
  invokestatic(0xb8, -2),
/**
  2: indexbyte1, indexbyte2 objectref, [arg1, arg2, ...] →  invoke virtual method on object objectref, where the method is identified by method reference index in constant pool (indexbyte1 << 8 + indexbyte2)
*/
  invokevirtual(0xb6, -3),
/**
  value1, value2 → result bitwise int or
*/
  ior(0x80, 2),
/**
  value1, value2 → result logical int remainder
*/
  irem(0x70, 2),
/**
  value → [empty] return an integer from a method
*/
  ireturn(0xac, 1),
/**
  value1, value2 → result int shift left
*/
  ishl(0x78, 2),
/**
  value1, value2 → result int arithmetic shift right
*/
  ishr(0x7a, 2),
/**
  1: index  value → store int value into variable #index
*/
  istore(0x36, 2),
/**
  value → store int value into variable 0
*/
  istore_0(0x3b, 1),
/**
  value → store int value into variable 1
*/
  istore_1(0x3c, 1),
/**
  value → store int value into variable 2
*/
  istore_2(0x3d, 1),
/**
  value → store int value into variable 3
*/
  istore_3(0x3e, 1),
/**
  value1, value2 → result int subtract
*/
  isub(0x64, 2),
/**
  value1, value2 → result int logical shift right
*/
  iushr(0x7c, 2),
/**
  value1, value2 → result int xor
*/
  ixor(0x82, 2),
/**
  2: branchbyte1, branchbyte2 → address jump to subroutine at branchoffset (signed short constructed from unsigned bytes branchbyte1 << 8 + branchbyte2) and place the return address on the stack
*/
  jsr(0xa8, 2),
/**
  4: branchbyte1, branchbyte2, branchbyte3, branchbyte4 → address jump to subroutine at branchoffset (signed int constructed from unsigned bytes branchbyte1 << 24 + branchbyte2 << 16 + branchbyte3 << 8 + branchbyte4) and place the return address on the stack
*/
  jsr_w(0xc9, 4),
/**
  value1, value2 → result add two longs
*/
  ladd(0x61, 2),
/**
  arrayref, index → value load a long from an array
*/
  laload(0x2f, 2),
/**
  value1, value2 → result bitwise and of two longs
*/
  land(0x7f, 2),
/**
  arrayref, index, value →  store a long to an array
*/
  lastore(0x50, 3),
/**
  value1, value2 → result compare two longs values
*/
  lcmp(0x94, 2),
/**
  → 0L  push the long 0 onto the stack
*/
  lconst_0(0x09, 0),
/**
  → 1L  push the long 1 onto the stack
*/
  lconst_1(0x0a, 0),
/**
  1: index  → value push a constant #index from a constant pool (String, int or float) onto the stack
*/
  ldc(0x12, 1),
/**
  2: indexbyte1, indexbyte2 → value push a constant #index from a constant pool (String, int or float) onto the stack (wide index is constructed as indexbyte1 << 8 + indexbyte2)
*/
  ldc_w(0x13, 2),
/**
  2: indexbyte1, indexbyte2 → value push a constant #index from a constant pool (double or long) onto the stack (wide index is constructed as indexbyte1 << 8 + indexbyte2)
*/
  ldc2_w(0x14, 2),
/**
  value1, value2 → result divide two longs
*/
  ldiv(0x6d, 2),
/**
  1: index  → value load a long value from a local variable #index
*/
  lload(0x16, 1),
/**
  → value load a long value from a local variable 0
*/
  lload_0(0x1e, 0),
/**
  → value load a long value from a local variable 1
*/
  lload_1(0x1f, 0),
/**
  → value load a long value from a local variable 2
*/
  lload_2(0x20, 0),
/**
  → value load a long value from a local variable 3
*/
  lload_3(0x21, 0),
/**
  value1, value2 → result multiply two longs
*/
  lmul(0x69, 2),
/**
  value → result  negate a long
*/
  lneg(0x75, 1),
/**
  4+: <0-3 bytes padding>, defaultbyte1, defaultbyte2, defaultbyte3, defaultbyte4, npairs1, npairs2, npairs3, npairs4, match-offset pairs...  key → a target address is looked up from a table using a key and execution continues from the instruction at that address
*/
  lookupswitch(0xab, -4),
/**
  value1, value2 → result bitwise or of two longs
*/
  lor(0x81, 2),
/**
  value1, value2 → result remainder of division of two longs
*/
  lrem(0x71, 2),
/**
  value → [empty] return a long value
*/
  lreturn(0xad, 1),
/**
  value1, value2 → result bitwise shift left of a long value1 by value2 positions
*/
  lshl(0x79, 2),
/**
  value1, value2 → result bitwise shift right of a long value1 by value2 positions
*/
  lshr(0x7b, 2),
/**
  1: index  value → store a long value in a local variable #index
*/
  lstore(0x37, 2),
/**
  value → store a long value in a local variable 0
*/
  lstore_0(0x3f, 0),
/**
  value → store a long value in a local variable 1
*/
  lstore_1(0x40, 1),
/**
  value → store a long value in a local variable 2
*/
  lstore_2(0x41, 1),
/**
  value → store a long value in a local variable 3
*/
  lstore_3(0x42, 1),
/**
  value1, value2 → result subtract two longs
*/
  lsub(0x65, 2),
/**
  value1, value2 → result bitwise shift right of a long value1 by value2 positions, unsigned
*/
  lushr(0x7d, 2),
/**
  value1, value2 → result bitwise exclusive or of two longs
*/
  lxor(0x83, 2),
/**
  objectref → enter monitor for object ("grab the lock" - start of synchronized() section)
*/
  monitorenter(0xc2, 1),
/**
  objectref → exit monitor for object ("release the lock" - end of synchronized() section)
*/
  monitorexit(0xc3, 1),
/**
  3: indexbyte1, indexbyte2, dimensions count1, [count2,...] → arrayref create a new array of dimensions dimensions with elements of type identified by class reference in constant pool index (indexbyte1 << 8 + indexbyte2); the sizes of each dimension is identified by count1, [count2, etc.]
*/
  multianewarray(0xc5, -4),
/**
  2: indexbyte1, indexbyte2 → objectref create new object of type identified by class reference in constant pool index (indexbyte1 << 8 + indexbyte2)
*/
  _new(0xbb, 2),
/**
  1: atype  count → arrayref  create new array with count elements of primitive type identified by atype
*/
  newarray(0xbc, 2),
/**
  [No change] perform no operation
*/
  nop(0, 0),
/**
  value → discard the top value on the stack
*/
  pop(0x57, 0),
/**
  {value2, value1} →  discard the top two values on the stack (or one value, if it is a double or long)
*/
  pop2(0x58, 2),
/**
  2: indexbyte1, indexbyte2 objectref, value →  set field to value in an object objectref, where the field is identified by a field reference index in constant pool (indexbyte1 << 8 + indexbyte2)
*/
  putfield(0xb5, 4),
/**
  2: indexbyte1, indexbyte2 value → set static field to value in a class, where the field is identified by a field reference index in constant pool (indexbyte1 << 8 + indexbyte2)
*/
  putstatic(0xb3, 3),
/**
  1: index  [No change] continue execution from address taken from a local variable #index (the asymmetry with jsr is intentional)
*/
  ret(0xa9, 1),
/**
  → [empty] return void from method
*/
  _return(0xb1, 0),
/**
  arrayref, index → value load short from array
*/
  saload(0x35, 2),
/**
  arrayref, index, value →  store short to array
*/
  sastore(0x56, 3),
/**
  : byte1, byte2 → value push a short onto the stack
*/
  sipush(0x11, 2),
/**
  value2, value1 → value1, value2 swaps two top words on the stack (note that value1 and value2 must not be double or long)
*/
  swap(0x5f, 2),
/**
  4+: [0-3 bytes padding], defaultbyte1, defaultbyte2, defaultbyte3, defaultbyte4, lowbyte1, lowbyte2, lowbyte3, lowbyte4, highbyte1, highbyte2, highbyte3, highbyte4, jump offsets...  index → continue execution from an address in the table at offset index
 */
  tableswitch(0xaa, -4),
/**
  3/5: opcode, indexbyte1, indexbyte2
  or
  iinc, indexbyte1, indexbyte2, countbyte1, countbyte2  [same as for corresponding instructions]  execute opcode, where opcode is either iload, fload, aload, lload, dload, istore, fstore, astore, lstore, dstore, or ret, but assume the index is 16 bit; or execute iinc, where the index is 16 bits and the constant to increment by is a signed 16 bit short
 */
  wide(0xc4, 3),
/**
  reserved for breakpoints in Java debuggers; should not appear in any class file
 */
  breakpoint(0xca, 0),
/**
  reserved for implementation-dependent operations within debuggers; should not appear in any class file
 */
  impdep1(0xfe, 0),
/**
  reserved for implementation-dependent operations within debuggers; should not appear in any class file
 */
  impdep2(0xff, 0),
  ;
  private final int code;
  private final int width;

  private ByteCodes(int code, int width) {
    this.code = code;
    this.width = width;
  }

  @Override
  public int code() {
    return code;
  }

  @Override
  public int width() {
    return width;
  }
}
