package org.rapla.entities.storage;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;
import org.rapla.entities.Entity;

/** returns if the entity is refering to the Object. */

public class ReferenceInfo<T extends Entity> implements Comparable
{
    final private String id;
    final private Class<? extends Entity> type;

    public ReferenceInfo(Entity entity) {
        this(entity.getId(), entity.getTypeClass());
    }

    public ReferenceInfo(String id, Class<? extends Entity> type) {
        super();
        this.id = id;
        this.type = type;
    }

    @JsMethod
    public String getId() {
        return id;
    }

    @JsMethod
    public Class<? extends Entity> getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if ( ! (obj instanceof ReferenceInfo))
        {
            return false;
        }
        return this.id.equals(((ReferenceInfo)obj).id) && type ==((ReferenceInfo)obj).type ;
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return type + ":" + id;
    }

    public boolean isReferenceOf(Entity object) {
        return id.equals(object.getId() );
    }

    /**
     * typesafe implementation of equals
     */
    public boolean isSame(ReferenceInfo<T> other)
    {
        return equals( other);
    }

    @Override public int compareTo(Object o)
    {
        return id.compareTo( ((ReferenceInfo) o).id);
    }
}
