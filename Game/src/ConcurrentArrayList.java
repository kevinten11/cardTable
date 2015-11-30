import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentArrayList<T> {

    /** use this to lock for write operations like add/remove */
    private final Lock readLock;
    /** use this to lock for read operations like get/iterator/contains.. */
    private final Lock writeLock;
    /** the underlying list*/
    private final List<T> list = new ArrayList<T>();

    {
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        readLock = rwLock.readLock();
        writeLock = rwLock.writeLock();
    }

    public void shuffle()
    {
    	writeLock.lock();
        try
        {
        	Collections.shuffle(list);
        }
        finally
        {
            writeLock.unlock();
        }
    }
    
    public void add(T e)
    {
        writeLock.lock();
        try
        {
            list.add(e);
        }
        finally
        {
            writeLock.unlock();
        }
    }
    
    public void remove(T e)
    {
    	writeLock.lock();
    	try
    	{
    		list.remove(e);
    	}
    	finally
    	{
    		writeLock.unlock();
    	}
    }
    
    public boolean contains(T e)
    {
    	readLock.lock();
    	try
    	{
    		return list.contains(e);
    	}
    	finally
    	{
    		readLock.unlock();
    	}
    }

	public T get(int index)
    {
        readLock.lock();
        try
        {
            return list.get(index);
        }
        finally
        {
            readLock.unlock();
        }
    }
	
	public int size()
	{
		readLock.lock();
		try
		{
			return list.size();
		}
		finally
		{
			readLock.unlock();
		}
	}
	
	public int indexOf(T e)
	{
		readLock.lock();
		try
		{
			return list.indexOf(e);
		}
		finally
		{
			readLock.unlock();
		}
	}

    public Iterator<T> iterator()
    {
        readLock.lock();
        try 
        {
            return new ArrayList<T>( list ).iterator();
                   //^ we iterate over an snapshot of our list 
        } 
        finally
        {
            readLock.unlock();
        }
    }
}
