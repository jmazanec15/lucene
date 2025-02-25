/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.util;

import java.util.Random;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.apache.lucene.tests.util.TestUtil;

public class TestVectorUtil extends LuceneTestCase {

  public static final double DELTA = 1e-4;

  public void testBasicDotProduct() {
    assertEquals(5, VectorUtil.dotProduct(new float[] {1, 2, 3}, new float[] {-10, 0, 5}), 0);
  }

  public void testSelfDotProduct() {
    // the dot product of a vector with itself is equal to the sum of the squares of its components
    float[] v = randomVector();
    assertEquals(l2(v), VectorUtil.dotProduct(v, v), DELTA);
  }

  public void testOrthogonalDotProduct() {
    // the dot product of two perpendicular vectors is 0
    float[] v = new float[2];
    v[0] = random().nextInt(100);
    v[1] = random().nextInt(100);
    float[] u = new float[2];
    u[0] = v[1];
    u[1] = -v[0];
    assertEquals(0, VectorUtil.dotProduct(u, v), DELTA);
  }

  public void testDotProductThrowsForDimensionMismatch() {
    float[] v = {1, 0, 0}, u = {0, 1};
    expectThrows(IllegalArgumentException.class, () -> VectorUtil.dotProduct(u, v));
  }

  public void testSelfSquareDistance() {
    // the l2 distance of a vector with itself is zero
    float[] v = randomVector();
    assertEquals(0, VectorUtil.squareDistance(v, v), DELTA);
  }

  public void testBasicSquareDistance() {
    assertEquals(12, VectorUtil.squareDistance(new float[] {1, 2, 3}, new float[] {-1, 0, 5}), 0);
  }

  public void testSquareDistanceThrowsForDimensionMismatch() {
    float[] v = {1, 0, 0}, u = {0, 1};
    expectThrows(IllegalArgumentException.class, () -> VectorUtil.squareDistance(u, v));
  }

  public void testRandomSquareDistance() {
    // the square distance of a vector with its inverse is equal to four times the sum of squares of
    // its components
    float[] v = randomVector();
    float[] u = negative(v);
    assertEquals(4 * l2(v), VectorUtil.squareDistance(u, v), DELTA);
  }

  public void testBasicCosine() {
    assertEquals(
        0.11952f, VectorUtil.cosine(new float[] {1, 2, 3}, new float[] {-10, 0, 5}), DELTA);
  }

  public void testSelfCosine() {
    // the dot product of a vector with itself is always equal to 1
    float[] v = randomVector();
    // ensure the vector is non-zero so that cosine is defined
    v[0] = random().nextFloat() + 0.01f;
    assertEquals(1.0f, VectorUtil.cosine(v, v), DELTA);
  }

  public void testOrthogonalCosine() {
    // the cosine of two perpendicular vectors is 0
    float[] v = new float[2];
    v[0] = random().nextInt(100);
    // ensure the vector is non-zero so that cosine is defined
    v[1] = random().nextInt(1, 100);
    float[] u = new float[2];
    u[0] = v[1];
    u[1] = -v[0];
    assertEquals(0, VectorUtil.cosine(u, v), DELTA);
  }

  public void testCosineThrowsForDimensionMismatch() {
    float[] v = {1, 0, 0}, u = {0, 1};
    expectThrows(IllegalArgumentException.class, () -> VectorUtil.cosine(u, v));
  }

  public void testNormalize() {
    float[] v = randomVector();
    v[random().nextInt(v.length)] = 1; // ensure vector is not all zeroes
    VectorUtil.l2normalize(v);
    assertEquals(1f, l2(v), DELTA);
  }

  public void testNormalizeZeroThrows() {
    float[] v = {0, 0, 0};
    expectThrows(IllegalArgumentException.class, () -> VectorUtil.l2normalize(v));
  }

  private static float l2(float[] v) {
    float l2 = 0;
    for (float x : v) {
      l2 += x * x;
    }
    return l2;
  }

  private static float[] negative(float[] v) {
    float[] u = new float[v.length];
    for (int i = 0; i < v.length; i++) {
      u[i] = -v[i];
    }
    return u;
  }

  private static BytesRef negative(BytesRef v) {
    BytesRef u = new BytesRef(new byte[v.length]);
    for (int i = 0; i < v.length; i++) {
      // what is (byte) -(-128)? 127?
      u.bytes[i] = (byte) -v.bytes[i];
    }
    return u;
  }

  private static float l2(BytesRef v) {
    float l2 = 0;
    for (int i = v.offset; i < v.offset + v.length; i++) {
      l2 += v.bytes[i] * v.bytes[i];
    }
    return l2;
  }

  private static float[] randomVector() {
    return randomVector(random().nextInt(100) + 1);
  }

  public static float[] randomVector(int dim) {
    float[] v = new float[dim];
    Random random = random();
    for (int i = 0; i < dim; i++) {
      v[i] = random.nextFloat();
    }
    return v;
  }

  private static BytesRef randomVectorBytes() {
    BytesRef v = TestUtil.randomBinaryTerm(random(), TestUtil.nextInt(random(), 1, 100));
    // clip at -127 to avoid overflow
    for (int i = v.offset; i < v.offset + v.length; i++) {
      if (v.bytes[i] == -128) {
        v.bytes[i] = -127;
      }
    }
    return v;
  }

  public void testBasicDotProductBytes() {
    BytesRef a = new BytesRef(new byte[] {1, 2, 3});
    BytesRef b = new BytesRef(new byte[] {-10, 0, 5});
    assertEquals(5, VectorUtil.dotProduct(a, b), 0);
    assertEquals(5 / (3f * (1 << 15)), VectorUtil.dotProductScore(a, b), DELTA);
  }

  public void testSelfDotProductBytes() {
    // the dot product of a vector with itself is equal to the sum of the squares of its components
    BytesRef v = randomVectorBytes();
    assertEquals(l2(v), VectorUtil.dotProduct(v, v), DELTA);
  }

  public void testOrthogonalDotProductBytes() {
    // the dot product of two perpendicular vectors is 0
    byte[] v = new byte[4];
    v[0] = (byte) random().nextInt(100);
    v[1] = (byte) random().nextInt(100);
    v[2] = v[1];
    v[3] = (byte) -v[0];
    // also test computing using BytesRef with nonzero offset
    assertEquals(0, VectorUtil.dotProduct(new BytesRef(v, 0, 2), new BytesRef(v, 2, 2)), DELTA);
  }

  public void testSelfSquareDistanceBytes() {
    // the l2 distance of a vector with itself is zero
    BytesRef v = randomVectorBytes();
    assertEquals(0, VectorUtil.squareDistance(v, v), DELTA);
  }

  public void testBasicSquareDistanceBytes() {
    assertEquals(
        12,
        VectorUtil.squareDistance(
            new BytesRef(new byte[] {1, 2, 3}), new BytesRef(new byte[] {-1, 0, 5})),
        0);
  }

  public void testRandomSquareDistanceBytes() {
    // the square distance of a vector with its inverse is equal to four times the sum of squares of
    // its components
    BytesRef v = randomVectorBytes();
    BytesRef u = negative(v);
    assertEquals(4 * l2(v), VectorUtil.squareDistance(u, v), DELTA);
  }

  public void testBasicCosineBytes() {
    assertEquals(
        0.11952f,
        VectorUtil.cosine(new BytesRef(new byte[] {1, 2, 3}), new BytesRef(new byte[] {-10, 0, 5})),
        DELTA);
  }

  public void testSelfCosineBytes() {
    // the dot product of a vector with itself is always equal to 1
    BytesRef v = randomVectorBytes();
    // ensure the vector is non-zero so that cosine is defined
    v.bytes[0] = (byte) (random().nextInt(126) + 1);
    assertEquals(1.0f, VectorUtil.cosine(v, v), DELTA);
  }

  public void testOrthogonalCosineBytes() {
    // the cosine of two perpendicular vectors is 0
    float[] v = new float[2];
    v[0] = random().nextInt(100);
    // ensure the vector is non-zero so that cosine is defined
    v[1] = random().nextInt(1, 100);
    float[] u = new float[2];
    u[0] = v[1];
    u[1] = -v[0];
    assertEquals(0, VectorUtil.cosine(u, v), DELTA);
  }
}
