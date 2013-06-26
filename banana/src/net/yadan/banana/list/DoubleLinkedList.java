package net.yadan.banana.list;

import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.block.BlockAllocator;
import net.yadan.banana.memory.malloc.ChainedAllocator;
import net.yadan.banana.memory.malloc.MultiSizeAllocator;


/**
 * @author omry
 * @created Apr 21, 2013
 */
public class DoubleLinkedList implements ILinkedList {

  private static final int PREV_OFFSET = 0;
  private static final int NEXT_OFFSET = 1;
  private static final int DATA_OFFSET = 2;
  public static final int RESERVED_SIZE = DATA_OFFSET;

  private int m_head;
  private int m_tail;
  private int m_size;
  private IMemAllocator m_memory;

  public DoubleLinkedList(int maxBlocks, int blockSize, double growthFactor) {
    init(new ChainedAllocator(maxBlocks, blockSize + RESERVED_SIZE, growthFactor));
  }

  public DoubleLinkedList(int maxBlocks, int sizes[], double growthFactor) {
    int sizes1[] = new int[sizes.length];
    for(int i=0;i<sizes.length;i++){
      sizes1[i] = sizes[i] + RESERVED_SIZE;
    }
    init(new MultiSizeAllocator(maxBlocks, sizes1, growthFactor));
  }

  public DoubleLinkedList(IMemAllocator memory) {
    init(memory);
  }

  protected void init(IMemAllocator memory) {
    m_memory = memory;
    m_head = -1;
    m_tail = -1;
    m_size = 0;
  }

  @Override
  public int insertHead(int size) {
    int link = m_memory.malloc(size + RESERVED_SIZE);
    m_memory.setInt(link, NEXT_OFFSET, m_head);
    m_memory.setInt(link, PREV_OFFSET, -1);
    if (m_head != -1) {
      m_memory.setInt(m_head, PREV_OFFSET, link);
    }
    m_head = link;
    if (m_tail == -1) {
      m_tail = link;
    }
    m_size++;
    return link;
  }

  @Override
  public int insert(int size, int anchor) {
    int link = m_memory.malloc(size + RESERVED_SIZE);
    if (m_head == -1 && anchor == m_head) {
      m_head = link;
      m_tail = link;
      m_memory.setInt(link, NEXT_OFFSET, -1);
      m_memory.setInt(link, PREV_OFFSET, -1);
    } else {
      int next = m_memory.getInt(anchor, NEXT_OFFSET);
      m_memory.setInt(link, NEXT_OFFSET, next);
      if (next != -1) {
        m_memory.setInt(next, PREV_OFFSET, link);
      }
      m_memory.setInt(link, PREV_OFFSET, anchor);
      m_memory.setInt(anchor, NEXT_OFFSET, link);
      if (anchor == m_tail) {
        m_tail = link;
      }
    }
    m_size++;
    return link;
  }

  @Override
  public void removeHead() {
    if (m_head != -1) {
      int new_head = getNext(m_head);

      if (m_head == m_tail) {
        m_tail = -1;
      } else {
        setPrev(new_head, -1);
      }

      m_memory.free(m_head);
      m_head = new_head;
      m_size--;
    }
  }

  @Override
  public int appendTail(int size) {
    int link = m_memory.malloc(size + RESERVED_SIZE);
    setNext(link, -1);
    setPrev(link, m_tail);

    if (m_head == -1) {
      m_head = link;
    } else {
      setNext(m_tail, link);
    }
    m_tail = link;
    m_size++;
    return link;
  }

  @Override
  public void remove(int link) {

    if (link == m_head) {
      m_head = getNext(link);
    }

    if (link == m_tail) {
      m_tail = getPrev(link);
    }

    if (getPrev(link) != -1) {
      setNext(getPrev(link), getNext(link));
    }

    if (getNext(link) != -1) {
      setPrev(getNext(link), getPrev(link));
    }

    m_size--;
    m_memory.free(link);
  }

  private void setNext(int link, int next) {
    m_memory.setInt(link, NEXT_OFFSET, next);
  }

  private void setPrev(int link, int prev) {
    m_memory.setInt(link, PREV_OFFSET, prev);
  }

  @Override
  public int getInt(int link, int offset_in_data) {
    return m_memory.getInt(link, DATA_OFFSET + offset_in_data);
  }

  @Override
  public long getLong(int link, int offset_in_data) {
    return m_memory.getLong(link, DATA_OFFSET + offset_in_data);
  }

  @Override
  public void setLong(int link, int offset_in_data, long data) {
    m_memory.setLong(link, DATA_OFFSET + offset_in_data, data);
  }

  @Override
  public void setInt(int link, int offset_in_data, int data) {
    m_memory.setInt(link, DATA_OFFSET + offset_in_data, data);
  }

  @Override
  public void setInts(int link, int dst_offset_in_record, int[] src_data, int src_pos, int length) {
    m_memory.setInts(link, DATA_OFFSET + dst_offset_in_record, src_data, src_pos, length);
  }

  @Override
  public void getInts(int link, int src_offset_in_record, int[] dst_data, int dst_pos, int length) {
    m_memory.getInts(link, DATA_OFFSET + src_offset_in_record, dst_data, dst_pos, length);
  }

  @Override
  public void getBuffer(int link, int src_offset_in_record, IBuffer dst, int length) {
    m_memory.getBuffer(link, DATA_OFFSET + src_offset_in_record, dst, length);
  }

  @Override
  public final int getHead() {
    return m_head;
  }

  @Override
  public final int getTail() {
    return m_tail;
  }

  @Override
  public int getNext(int link) {
    return m_memory.getInt(link, NEXT_OFFSET);
  }

  @Override
  public int getPrev(int pointer) {
    return m_memory.getInt(pointer, PREV_OFFSET);
  }

  public void clear() {
    m_head = m_tail = -1;
  }

  @Override
  public IMemAllocator getAllocator() {
    return m_memory;
  }

  public static int getIntArraySize(int maxBlocks, int recordSize) {
    return BlockAllocator.getIntArraySize(maxBlocks, recordSize + RESERVED_SIZE);
  }

  @Override
  public int size() {
    return m_size;
  }


  @Override
  public int insertHead(IBuffer data) {
    int ret = insertHead(data.size());
    setInts(ret, 0, data.array(), 0, data.size());
    return ret;
  }

  @Override
  public int insert(IBuffer data, int anchor) {
    int ret = insert(data.size(), anchor);
    setInts(ret, 0, data.array(), 0, data.size());
    return ret;
  }

  @Override
  public int appendTail(IBuffer data) {
    int ret = appendTail(data.size());
    setInts(ret, 0, data.array(), 0, data.size());
    return ret;
  }
}