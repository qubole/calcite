/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.protobuf;

/**
 * Helper class to extract byte arrays from {@link ByteString} without copy.
 *
 * Without this protobufs would force us to copy every single byte array out
 * of the objects de-serialized from the wire (which already do one copy, on
 * top of the copies the JVM does to go from kernel buffer to C buffer and
 * from C buffer to JVM buffer).
 *
 * Graciously copied from Apache HBase.
 */
public final class HBaseZeroCopyByteString extends LiteralByteString {
  // Gotten from AsyncHBase code base with permission.
  /** Private constructor so this class cannot be instantiated. */
  private HBaseZeroCopyByteString() {
    super(null);
    throw new UnsupportedOperationException("Should never be here.");
  }

  /**
   * Wraps a byte array in a {@link ByteString} without copying it.
   *
   * @param array The byte array to wrap
   * @return a ByteString wrapping the <code>array</code>
   */
  public static ByteString wrap(final byte[] array) {
    return new LiteralByteString(array);
  }

  /**
   * Wraps a subset of a byte array in a {@link ByteString} without copying it.
   *
   * @param array The byte array to wrap
   * @param offset the start of data in the array
   * @param length The number of bytes of data at <code>offset</code>
   * @return a ByteString wrapping the <code>array</code>
   */
  public static ByteString wrap(final byte[] array, int offset, int length) {
    return new BoundedByteString(array, offset, length);
  }


  /**
   * Extracts the byte array from the given {@link ByteString} without copy.
   * @param buf A buffer from which to extract the array.  This buffer must be
   * actually an instance of a {@code LiteralByteString}.
   *
   * @param buf <code>ByteString</code> to access
   * @return The underlying byte array of the ByteString
   */
  public static byte[] zeroCopyGetBytes(final ByteString buf) {
    if (buf instanceof LiteralByteString) {
      return ((LiteralByteString) buf).bytes;
    }
    throw new UnsupportedOperationException("Need a LiteralByteString, got a "
                                            + buf.getClass().getName());
  }
}

// End HBaseZeroCopyByteString.java
