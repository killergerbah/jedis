package redis.clients.jedis.commands.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.AssertUtil;
import redis.clients.jedis.util.JedisByteHashMap;

public abstract class HashesCommandsTestBase extends UnifiedJedisCommandsTestBase {

  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };

  final byte[] bbar1 = { 0x05, 0x06, 0x07, 0x08, 0x0A };
  final byte[] bbar2 = { 0x05, 0x06, 0x07, 0x08, 0x0B };
  final byte[] bbar3 = { 0x05, 0x06, 0x07, 0x08, 0x0C };
  final byte[] bbarstar = { 0x05, 0x06, 0x07, 0x08, '*' };

  @Test
  public void hset() {
    assertEquals(1, jedis.hset("foo", "bar", "car"));
    assertEquals(0, jedis.hset("foo", "bar", "foo"));

    // Binary
    assertEquals(1, jedis.hset(bfoo, bbar, bcar));
    assertEquals(0, jedis.hset(bfoo, bbar, bfoo));
  }

  @Test
  public void hget() {
    jedis.hset("foo", "bar", "car");
    assertNull(jedis.hget("bar", "foo"));
    assertNull(jedis.hget("foo", "car"));
    assertEquals("car", jedis.hget("foo", "bar"));

    // Binary
    jedis.hset(bfoo, bbar, bcar);
    assertNull(jedis.hget(bbar, bfoo));
    assertNull(jedis.hget(bfoo, bcar));
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));
  }

  @Test
  public void hsetnx() {
    assertEquals(1, jedis.hsetnx("foo", "bar", "car"));
    assertEquals("car", jedis.hget("foo", "bar"));

    assertEquals(0, jedis.hsetnx("foo", "bar", "foo"));
    assertEquals("car", jedis.hget("foo", "bar"));

    assertEquals(1, jedis.hsetnx("foo", "car", "bar"));
    assertEquals("bar", jedis.hget("foo", "car"));

    // Binary
    assertEquals(1, jedis.hsetnx(bfoo, bbar, bcar));
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));

    assertEquals(0, jedis.hsetnx(bfoo, bbar, bfoo));
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));

    assertEquals(1, jedis.hsetnx(bfoo, bcar, bbar));
    assertArrayEquals(bbar, jedis.hget(bfoo, bcar));
  }

  @Test
  public void hmset() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    assertEquals("OK", jedis.hmset("foo", hash));
    assertEquals("car", jedis.hget("foo", "bar"));
    assertEquals("bar", jedis.hget("foo", "car"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    assertEquals("OK", jedis.hmset(bfoo, bhash));
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));
    assertArrayEquals(bbar, jedis.hget(bfoo, bcar));
  }

  @Test
  public void hsetVariadic() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    assertEquals(2, jedis.hset("foo", hash));
    assertEquals("car", jedis.hget("foo", "bar"));
    assertEquals("bar", jedis.hget("foo", "car"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    assertEquals(2, jedis.hset(bfoo, bhash));
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));
    assertArrayEquals(bbar, jedis.hget(bfoo, bcar));
  }

  @Test
  public void hmget() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    jedis.hmset("foo", hash);

    List<String> values = jedis.hmget("foo", "bar", "car", "foo");
    List<String> expected = new ArrayList<String>();
    expected.add("car");
    expected.add("bar");
    expected.add(null);

    assertEquals(expected, values);

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    jedis.hmset(bfoo, bhash);

    List<byte[]> bvalues = jedis.hmget(bfoo, bbar, bcar, bfoo);
    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bcar);
    bexpected.add(bbar);
    bexpected.add(null);

    AssertUtil.assertByteArrayListEquals(bexpected, bvalues);
  }

  @Test
  public void hincrBy() {
    assertEquals(1, jedis.hincrBy("foo", "bar", 1));
    assertEquals(0, jedis.hincrBy("foo", "bar", -1));
    assertEquals(-10, jedis.hincrBy("foo", "bar", -10));

    // Binary
    assertEquals(1, jedis.hincrBy(bfoo, bbar, 1));
    assertEquals(0, jedis.hincrBy(bfoo, bbar, -1));
    assertEquals(-10, jedis.hincrBy(bfoo, bbar, -10));
  }

  @Test
  public void hincrByFloat() {
    assertEquals(1.5d, jedis.hincrByFloat("foo", "bar", 1.5d), 0);
    assertEquals(0d, jedis.hincrByFloat("foo", "bar", -1.5d), 0);
    assertEquals(-10.7d, jedis.hincrByFloat("foo", "bar", -10.7d), 0);

    // Binary
    assertEquals(1.5d, jedis.hincrByFloat(bfoo, bbar, 1.5d), 0d);
    assertEquals(0d, jedis.hincrByFloat(bfoo, bbar, -1.5d), 0d);
    assertEquals(-10.7d, jedis.hincrByFloat(bfoo, bbar, -10.7d), 0d);
  }

  @Test
  public void hexists() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    jedis.hmset("foo", hash);

    assertFalse(jedis.hexists("bar", "foo"));
    assertFalse(jedis.hexists("foo", "foo"));
    assertTrue(jedis.hexists("foo", "bar"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    jedis.hmset(bfoo, bhash);

    assertFalse(jedis.hexists(bbar, bfoo));
    assertFalse(jedis.hexists(bfoo, bfoo));
    assertTrue(jedis.hexists(bfoo, bbar));
  }

  @Test
  public void hdel() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    jedis.hmset("foo", hash);

    assertEquals(0, jedis.hdel("bar", "foo"));
    assertEquals(0, jedis.hdel("foo", "foo"));
    assertEquals(1, jedis.hdel("foo", "bar"));
    assertNull(jedis.hget("foo", "bar"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    jedis.hmset(bfoo, bhash);

    assertEquals(0, jedis.hdel(bbar, bfoo));
    assertEquals(0, jedis.hdel(bfoo, bfoo));
    assertEquals(1, jedis.hdel(bfoo, bbar));
    assertNull(jedis.hget(bfoo, bbar));
  }

  @Test
  public void hlen() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    jedis.hmset("foo", hash);

    assertEquals(0, jedis.hlen("bar"));
    assertEquals(2, jedis.hlen("foo"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    jedis.hmset(bfoo, bhash);

    assertEquals(0, jedis.hlen(bbar));
    assertEquals(2, jedis.hlen(bfoo));
  }

  @Test
  public void hkeys() {
    Map<String, String> hash = new LinkedHashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    jedis.hmset("foo", hash);

    Set<String> keys = jedis.hkeys("foo");
    Set<String> expected = new LinkedHashSet<String>();
    expected.add("bar");
    expected.add("car");
    assertEquals(expected, keys);

    // Binary
    Map<byte[], byte[]> bhash = new LinkedHashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    jedis.hmset(bfoo, bhash);

    Set<byte[]> bkeys = jedis.hkeys(bfoo);
    Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bbar);
    bexpected.add(bcar);
    AssertUtil.assertByteArraySetEquals(bexpected, bkeys);
  }

  @Test
  public void hvals() {
    Map<String, String> hash = new LinkedHashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    jedis.hmset("foo", hash);

    List<String> vals = jedis.hvals("foo");
    assertEquals(2, vals.size());
    AssertUtil.assertCollectionContains(vals, "bar");
    AssertUtil.assertCollectionContains(vals, "car");

    // Binary
    Map<byte[], byte[]> bhash = new LinkedHashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    jedis.hmset(bfoo, bhash);

    List<byte[]> bvals = jedis.hvals(bfoo);

    assertEquals(2, bvals.size());
    AssertUtil.assertByteArrayCollectionContains(bvals, bbar);
    AssertUtil.assertByteArrayCollectionContains(bvals, bcar);
  }

  @Test
  public void hgetAll() {
    Map<String, String> h = new HashMap<String, String>();
    h.put("bar", "car");
    h.put("car", "bar");
    jedis.hmset("foo", h);

    Map<String, String> hash = jedis.hgetAll("foo");
    assertEquals(2, hash.size());
    assertEquals("car", hash.get("bar"));
    assertEquals("bar", hash.get("car"));

    // Binary
    Map<byte[], byte[]> bh = new HashMap<byte[], byte[]>();
    bh.put(bbar, bcar);
    bh.put(bcar, bbar);
    jedis.hmset(bfoo, bh);
    Map<byte[], byte[]> bhash = jedis.hgetAll(bfoo);

    assertEquals(2, bhash.size());
    assertArrayEquals(bcar, bhash.get(bbar));
    assertArrayEquals(bbar, bhash.get(bcar));
  }

  @Test
  public void hscan() {
    jedis.hset("foo", "b", "y");
    jedis.hset("foo", "a", "x");

    ScanResult<Map.Entry<String, String>> result = jedis.hscan("foo", SCAN_POINTER_START);

    assertEquals(SCAN_POINTER_START, result.getCursor());
    assertEquals(2, result.getResult().size());

    assertThat(
        result.getResult().stream().map(Map.Entry::getKey).collect(Collectors.toList()),
        containsInAnyOrder("a", "b"));
    assertThat(
        result.getResult().stream().map(Map.Entry::getValue).collect(Collectors.toList()),
        containsInAnyOrder("x", "y"));

    // binary
    jedis.hset(bfoo, bbar, bcar);

    ScanResult<Map.Entry<byte[], byte[]>> bResult = jedis.hscan(bfoo, SCAN_POINTER_START_BINARY);

    assertArrayEquals(SCAN_POINTER_START_BINARY, bResult.getCursorAsBytes());
    assertEquals(1, bResult.getResult().size());

    assertThat(
        bResult.getResult().stream().map(Map.Entry::getKey).collect(Collectors.toList()),
        containsInAnyOrder(bbar));
    assertThat(
        bResult.getResult().stream().map(Map.Entry::getValue).collect(Collectors.toList()),
        containsInAnyOrder(bcar));
  }

  @Test
  public void hscanMatch() {
    ScanParams params = new ScanParams();
    params.match("a*");

    jedis.hset("foo", "b", "y");
    jedis.hset("foo", "a", "x");
    jedis.hset("foo", "aa", "xx");
    ScanResult<Map.Entry<String, String>> result = jedis.hscan("foo", SCAN_POINTER_START, params);

    assertEquals(SCAN_POINTER_START, result.getCursor());
    assertEquals(2, result.getResult().size());

    assertThat(
        result.getResult().stream().map(Map.Entry::getKey).collect(Collectors.toList()),
        containsInAnyOrder("a", "aa"));
    assertThat(
        result.getResult().stream().map(Map.Entry::getValue).collect(Collectors.toList()),
        containsInAnyOrder("x", "xx"));

    // binary
    params = new ScanParams();
    params.match(bbarstar);

    jedis.hset(bfoo, bbar, bcar);
    jedis.hset(bfoo, bbar1, bcar);
    jedis.hset(bfoo, bbar2, bcar);
    jedis.hset(bfoo, bbar3, bcar);

    ScanResult<Map.Entry<byte[], byte[]>> bResult = jedis.hscan(bfoo, SCAN_POINTER_START_BINARY, params);

    assertArrayEquals(SCAN_POINTER_START_BINARY, bResult.getCursorAsBytes());
    assertEquals(4, bResult.getResult().size());

    assertThat(
        bResult.getResult().stream().map(Map.Entry::getKey).collect(Collectors.toList()),
        containsInAnyOrder(bbar, bbar1, bbar2, bbar3));
    assertThat(
        bResult.getResult().stream().map(Map.Entry::getValue).collect(Collectors.toList()),
        containsInAnyOrder(bcar, bcar, bcar, bcar));
  }

  @Test
  public void hscanCount() {
    ScanParams params = new ScanParams();
    params.count(2);

    for (int i = 0; i < 10; i++) {
      jedis.hset("foo", "a" + i, "x" + i);
    }

    ScanResult<Map.Entry<String, String>> result = jedis.hscan("foo", SCAN_POINTER_START, params);

    assertFalse(result.getResult().isEmpty());

    assertThat(
        result.getResult().stream().map(Map.Entry::getKey).map(s -> s.substring(0, 1)).collect(Collectors.toSet()),
        containsInAnyOrder("a"));
    assertThat(
        result.getResult().stream().map(Map.Entry::getValue).map(s -> s.substring(0, 1)).collect(Collectors.toSet()),
        containsInAnyOrder("x"));

    // binary
    params = new ScanParams();
    params.count(2);

    jedis.hset(bfoo, bbar, bcar);
    jedis.hset(bfoo, bbar1, bcar);
    jedis.hset(bfoo, bbar2, bcar);
    jedis.hset(bfoo, bbar3, bcar);

    ScanResult<Map.Entry<byte[], byte[]>> bResult = jedis.hscan(bfoo, SCAN_POINTER_START_BINARY, params);

    assertFalse(bResult.getResult().isEmpty());

    assertThat(
        bResult.getResult().stream().map(Map.Entry::getKey)
            .map(a -> Arrays.copyOfRange(a, 0, 4)).map(Arrays::toString).collect(Collectors.toSet()),
        containsInAnyOrder(Arrays.toString(bbar)));
    assertThat(
        bResult.getResult().stream().map(Map.Entry::getValue)
            .map(a -> Arrays.copyOfRange(a, 0, 4)).map(Arrays::toString).collect(Collectors.toSet()),
        containsInAnyOrder(Arrays.toString(bcar)));
  }

  @Test
  public void hscanNoValues() {
    jedis.hset("foo", "b", "y");
    jedis.hset("foo", "a", "x");

    ScanResult<String> result = jedis.hscanNoValues("foo", SCAN_POINTER_START);

    assertEquals(SCAN_POINTER_START, result.getCursor());
    assertEquals(2, result.getResult().size());

    assertThat(result.getResult(), containsInAnyOrder("a", "b"));

    // binary
    jedis.hset(bfoo, bbar, bcar);

    ScanResult<byte[]> bResult = jedis.hscanNoValues(bfoo, SCAN_POINTER_START_BINARY);

    assertArrayEquals(SCAN_POINTER_START_BINARY, bResult.getCursorAsBytes());
    assertEquals(1, bResult.getResult().size());

    assertThat(bResult.getResult(), containsInAnyOrder(bbar));
  }

  @Test
  public void hscanNoValuesMatch() {
    ScanParams params = new ScanParams();
    params.match("a*");

    jedis.hset("foo", "b", "y");
    jedis.hset("foo", "a", "x");
    jedis.hset("foo", "aa", "xx");
    ScanResult<String> result = jedis.hscanNoValues("foo", SCAN_POINTER_START, params);

    assertEquals(SCAN_POINTER_START, result.getCursor());
    assertEquals(2, result.getResult().size());

    assertThat(result.getResult(), containsInAnyOrder("a", "aa"));

    // binary
    params = new ScanParams();
    params.match(bbarstar);

    jedis.hset(bfoo, bbar, bcar);
    jedis.hset(bfoo, bbar1, bcar);
    jedis.hset(bfoo, bbar2, bcar);
    jedis.hset(bfoo, bbar3, bcar);

    ScanResult<byte[]> bResult = jedis.hscanNoValues(bfoo, SCAN_POINTER_START_BINARY, params);

    assertArrayEquals(SCAN_POINTER_START_BINARY, bResult.getCursorAsBytes());
    assertEquals(4, bResult.getResult().size());

    assertThat(bResult.getResult(), containsInAnyOrder(bbar, bbar1, bbar2, bbar3));
  }

  @Test
  public void hscanNoValuesCount() {
    ScanParams params = new ScanParams();
    params.count(2);

    for (int i = 0; i < 10; i++) {
      jedis.hset("foo", "a" + i, "a" + i);
    }

    ScanResult<String> result = jedis.hscanNoValues("foo", SCAN_POINTER_START, params);

    assertFalse(result.getResult().isEmpty());

    assertThat(
        result.getResult().stream().map(s -> s.substring(0, 1)).collect(Collectors.toSet()),
        containsInAnyOrder("a"));

    // binary
    params = new ScanParams();
    params.count(2);

    jedis.hset(bfoo, bbar, bcar);
    jedis.hset(bfoo, bbar1, bcar);
    jedis.hset(bfoo, bbar2, bcar);
    jedis.hset(bfoo, bbar3, bcar);

    ScanResult<byte[]> bResult = jedis.hscanNoValues(bfoo, SCAN_POINTER_START_BINARY, params);

    assertFalse(bResult.getResult().isEmpty());

    assertThat(
        bResult.getResult().stream()
            .map(a -> Arrays.copyOfRange(a, 0, 4)).map(Arrays::toString).collect(Collectors.toSet()),
        containsInAnyOrder(Arrays.toString(bbar)));
  }

  @Test
  public void testHstrLen_EmptyHash() {
    Long response = jedis.hstrlen("myhash", "k1");
    assertEquals(0l, response.longValue());
  }

  @Test
  public void testHstrLen() {
    Map<String, String> values = new HashMap<>();
    values.put("key", "value");
    jedis.hmset("myhash", values);
    Long response = jedis.hstrlen("myhash", "key");
    assertEquals(5l, response.longValue());

  }

  @Test
  public void testBinaryHstrLen() {
    Map<byte[], byte[]> values = new HashMap<>();
    values.put(bbar, bcar);
    jedis.hmset(bfoo, values);
    Long response = jedis.hstrlen(bfoo, bbar);
    assertEquals(4l, response.longValue());
  }

  @Test
  public void hrandfield() {
    assertNull(jedis.hrandfield("foo"));
    assertEquals(Collections.emptyList(), jedis.hrandfield("foo", 1));
    assertEquals(Collections.emptyList(), jedis.hrandfieldWithValues("foo", 1));
    assertEquals(Collections.emptyList(), jedis.hrandfieldWithValues("foo", -1));

    Map<String, String> hash = new LinkedHashMap<>();
    hash.put("bar", "bar");
    hash.put("car", "car");
    hash.put("bar1", "bar1");

    jedis.hset("foo", hash);

    assertTrue(hash.containsKey(jedis.hrandfield("foo")));
    assertEquals(2, jedis.hrandfield("foo", 2).size());

    List<Map.Entry<String, String>> actual = jedis.hrandfieldWithValues("foo", 2);
    assertEquals(2, actual.size());
    actual.forEach(e -> assertEquals(hash.get(e.getKey()), e.getValue()));

    actual = jedis.hrandfieldWithValues("foo", 5);
    assertEquals(3, actual.size());
    actual.forEach(e -> assertEquals(hash.get(e.getKey()), e.getValue()));

    actual = jedis.hrandfieldWithValues("foo", -5);
    assertEquals(5, actual.size());
    actual.forEach(e -> assertEquals(hash.get(e.getKey()), e.getValue()));

    // binary
    assertNull(jedis.hrandfield(bfoo));
    assertEquals(Collections.emptyList(), jedis.hrandfield(bfoo, 1));
    assertEquals(Collections.emptyList(), jedis.hrandfieldWithValues(bfoo, 1));
    assertEquals(Collections.emptyList(), jedis.hrandfieldWithValues(bfoo, -1));

    Map<byte[], byte[]> bhash = new JedisByteHashMap();
    bhash.put(bbar, bbar);
    bhash.put(bcar, bcar);
    bhash.put(bbar1, bbar1);

    jedis.hset(bfoo, bhash);

    assertTrue(bhash.containsKey(jedis.hrandfield(bfoo)));
    assertEquals(2, jedis.hrandfield(bfoo, 2).size());

    List<Map.Entry<byte[], byte[]>> bactual = jedis.hrandfieldWithValues(bfoo, 2);
    assertEquals(2, bactual.size());
    bactual.forEach(e -> assertArrayEquals(bhash.get(e.getKey()), e.getValue()));

    bactual = jedis.hrandfieldWithValues(bfoo, 5);
    assertEquals(3, bactual.size());
    bactual.forEach(e -> assertArrayEquals(bhash.get(e.getKey()), e.getValue()));

    bactual = jedis.hrandfieldWithValues(bfoo, -5);
    assertEquals(5, bactual.size());
    bactual.forEach(e -> assertArrayEquals(bhash.get(e.getKey()), e.getValue()));
  }
}
