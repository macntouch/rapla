/*--------------------------------------------------------------------------*
 | Copyright (C) 2014 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.storage;

import org.rapla.components.util.Assert;
import org.rapla.entities.Category;
import org.rapla.entities.Entity;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.internal.PreferencesImpl;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.internal.AllocatableImpl;
import org.rapla.entities.domain.internal.ReservationImpl;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.internal.DynamicTypeImpl;
import org.rapla.entities.internal.CategoryImpl;
import org.rapla.entities.internal.UserImpl;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.ParentEntity;
import org.rapla.entities.storage.internal.SimpleEntity;
import org.rapla.facade.Conflict;
import org.rapla.facade.internal.ConflictImpl;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalCache implements EntityResolver
{
    Map<String, String> passwords = new HashMap<String, String>();
    Map<String, Entity> entities;

    //Map<String,ConflictImpl> disabledConflicts = new HashMap<String,ConflictImpl>();
    Set<String> disabledConflictApp1 = new HashSet<String>();
    Set<String> disabledConflictApp2 = new HashSet<String>();
    Map<String,Date> conflictLastChanged = new HashMap<String,Date>();

    Map<String, DynamicTypeImpl> dynamicTypes;
    Map<String, UserImpl> users;
    Map<String, AllocatableImpl> resources;
    Map<String, ReservationImpl> reservations;

    private String clientUserId;
    private final PermissionController permissionController;

    public LocalCache(PermissionController permissionController)
    {
        this.permissionController = permissionController;
        entities = new HashMap<String, Entity>();
        // top-level-entities
        reservations = new LinkedHashMap<String, ReservationImpl>();
        users = new LinkedHashMap<String, UserImpl>();
        resources = new LinkedHashMap<String, AllocatableImpl>();
        dynamicTypes = new LinkedHashMap<String, DynamicTypeImpl>();
        initSuperCategory();
    }

    public String getClientUserId()
    {
        return clientUserId;
    }

    /** use this to prohibit reservations and preferences (except from system and current user) to be stored in the cache*/
    public void setClientUserId(String clientUserId)
    {
        this.clientUserId = clientUserId;
    }

    /** @return true if the entity has been removed and false if the entity was not found*/
    public boolean remove(Entity entity)
    {
        if (entity instanceof ParentEntity)
        {
            Collection<Entity> subEntities = ((ParentEntity) entity).getSubEntities();
            for (Entity child : subEntities)
            {
                remove(child);
            }
        }
        RaplaType raplaType = entity.getRaplaType();
        String entityId = entity.getId();
        return removeWithId(raplaType, entityId);
    }

    /** WARNING child entities will not be removed if you use this method */
    public boolean removeWithId(RaplaType raplaType, String entityId)
    {
        boolean bResult = true;
        bResult = entities.remove(entityId) != null;
        Map<String, ? extends Entity> entitySet = getMap(raplaType);
        if (entitySet != null)
        {
            if (entityId == null)
                return false;
            entitySet.remove(entityId);
        }
        else if (raplaType == Conflict.TYPE)
        {
            disabledConflictApp1.remove(entityId);
            disabledConflictApp2.remove(entityId);
        }
        return bResult;
    }

    @SuppressWarnings("unchecked") private Map<String, Entity> getMap(RaplaType type)
    {
        if (type == Reservation.TYPE)
        {
            return (Map) reservations;
        }
        if (type == Allocatable.TYPE)
        {
            return (Map) resources;
        }
        if (type == DynamicType.TYPE)
        {
            return (Map) dynamicTypes;
        }
        if (type == User.TYPE)
        {
            return (Map) users;
        }
        return null;
    }

    public void put(Entity entity)
    {
        Assert.notNull(entity);

        RaplaType raplaType = entity.getRaplaType();

        String entityId = entity.getId();
        if (entityId == null)
            throw new IllegalStateException("ID can't be null");

        String clientUserId = getClientUserId();
        if (clientUserId != null)
        {
            if (raplaType == Reservation.TYPE || raplaType == Appointment.TYPE)
            {
                throw new IllegalArgumentException("Can't store reservations, appointments or conflicts in client cache");
            }
            // we ignore client stores for now
            if (raplaType == Conflict.TYPE)
            {
                return;
            }
            if (raplaType == Preferences.TYPE)
            {
                String owner = ((PreferencesImpl) entity).getId("owner");
                if (owner != null && !owner.equals(clientUserId))
                {
                    throw new IllegalArgumentException("Can't store non system preferences for other users in client cache");
                }
            }
        }

        // first remove the old children from the map
        Entity oldEntity = entities.get(entity);
        if (oldEntity != null && oldEntity instanceof ParentEntity)
        {
            Collection<Entity> subEntities = ((ParentEntity) oldEntity).getSubEntities();
            for (Entity child : subEntities)
            {
                remove(child);
            }
        }

        entities.put(entityId, entity);
        Map<String, Entity> entitySet = getMap(raplaType);
        if (entitySet != null)
        {
            entitySet.put(entityId, entity);
        }
        else if (entity instanceof Conflict)
        {
            Conflict conflict = (Conflict) entity;
            if (conflict.isAppointment1Enabled())
            {
                disabledConflictApp1.remove(entityId);
            }
            else
            {
                disabledConflictApp1.add(entityId);
            }
            if (conflict.isAppointment2Enabled())
            {
                disabledConflictApp2.remove(entityId);
            }
            else
            {
                disabledConflictApp2.add(entityId);
            }
            final Date lastChanged = conflict.getLastChanged();
            conflictLastChanged.put( entityId, lastChanged);
            if ( conflict.isAppointment1Enabled() && conflict.isAppointment2Enabled())
            {
                conflictLastChanged.remove( entityId);
            }
        }
        else
        {
            //throw new RuntimeException("UNKNOWN TYPE. Can't store object in cache: " + entity.getRaplaType());
        }
        // then put the new children
        if (entity instanceof ParentEntity)
        {
            Collection<Entity> subEntities = ((ParentEntity) entity).getSubEntities();
            for (Entity child : subEntities)
            {
                put(child);
            }
        }
    }

    public Entity get(Comparable id)
    {
        if (id == null)
            throw new RuntimeException("id is null");
        return entities.get(id);
    }

    //    @SuppressWarnings("unchecked")
    //    private <T extends Entity> Collection<T> getCollection(RaplaType type) {
    //        Map<String,? extends Entity> entities =  entityMap.get(type);
    //
    //        if (entities != null) {
    //            return (Collection<T>) entities.values();
    //        } else {
    //            throw new RuntimeException("UNKNOWN TYPE. Can't get collection: "
    //                                       +  type);
    //        }
    //    }
    //
    //    @SuppressWarnings("unchecked")
    //    private <T extends RaplaObject> Collection<T> getCollection(Class<T> clazz) {
    //    	RaplaType type = RaplaType.get(clazz);
    //		Collection<T> collection = (Collection<T>) getCollection(type);
    //		return new LinkedHashSet(collection);
    //    }

    public void clearAll()
    {
        passwords.clear();
        reservations.clear();
        users.clear();
        resources.clear();
        dynamicTypes.clear();
        entities.clear();
        disabledConflictApp1.clear();
        disabledConflictApp2.clear();
        conflictLastChanged.clear();
        initSuperCategory();
    }

    private void initSuperCategory()
    {
        CategoryImpl superCategory = new CategoryImpl(null, null);
        superCategory.setId(Category.SUPER_CATEGORY_ID);
        superCategory.setKey("supercategory");
        superCategory.getName().setName("en", "Root");
        entities.put(Category.SUPER_CATEGORY_ID, superCategory);
        Category[] childs = superCategory.getCategories();
        for (int i = 0; i < childs.length; i++)
        {
            superCategory.removeCategory(childs[i]);
        }
    }

    public CategoryImpl getSuperCategory()
    {
        return (CategoryImpl) get(Category.SUPER_CATEGORY_ID);
    }

    public UserImpl getUser(String username)
    {
        for (UserImpl user : users.values())
        {
            if (user.getUsername().equals(username))
                return user;
        }
        for (UserImpl user : users.values())
        {
            if (user.getUsername().equalsIgnoreCase(username))
                return user;
        }
        return null;
    }

    public PreferencesImpl getPreferencesForUserId(String userId)
    {
        String preferenceId = PreferencesImpl.getPreferenceIdFromUser(userId);
        PreferencesImpl pref = (PreferencesImpl) tryResolve(preferenceId, Preferences.class);
        return pref;
    }

    public DynamicType getDynamicType(String elementKey)
    {
        for (DynamicType dt : dynamicTypes.values())
        {
            if (dt.getKey().equals(elementKey))
                return dt;
        }
        return null;
    }

    public List<Entity> getVisibleEntities(final User user)
    {
        List<Entity> result = new ArrayList<Entity>();
        result.add(getSuperCategory());
        result.addAll(getDynamicTypes());
        result.addAll(getUsers());
        for (Allocatable alloc : getAllocatables())
        {
            if (user == null || user.isAdmin() || permissionController.canReadOnlyInformation(alloc, user))
            {
                result.add(alloc);
            }
        }
        // add system preferences
        {
            PreferencesImpl preferences = getPreferencesForUserId(null);
            if (preferences != null)
            {
                result.add(preferences);
            }
        }
        // add user preferences
        if (user != null)
        {
            String userId = user.getId();
            Assert.notNull(userId);
            PreferencesImpl preferences = getPreferencesForUserId(userId);
            if (preferences != null)
            {
                result.add(preferences);
            }
        }
        return result;
    }

    // Implementation of EntityResolver
    @Override public Entity resolve(String id) throws EntityNotFoundException
    {
        return resolve(id, null);
    }

    public <T extends Entity> T resolve(String id, Class<T> entityClass) throws EntityNotFoundException
    {
        T entity = tryResolve(id, entityClass);
        SimpleEntity.checkResolveResult(id, entityClass, entity);
        return entity;
    }

    @Override public Entity tryResolve(String id)
    {
        return tryResolve(id, null);
    }

    @Override public <T extends Entity> T tryResolve(String id, Class<T> entityClass)
    {
        if (id == null)
            throw new RuntimeException("id is null");
        Entity entity = entities.get(id);
        @SuppressWarnings("unchecked") T casted = (T) entity;
        return casted;
    }

    public String getPassword(String userId)
    {
        return passwords.get(userId);
    }

    public void putPassword(String userId, String password)
    {
        passwords.put(userId, password);
    }

    public void putAll(Collection<? extends Entity> list)
    {
        for (Entity entity : list)
        {
            put(entity);
        }
    }

    public Provider<Category> getSuperCategoryProvider()
    {
        return new Provider<Category>()
        {

            public Category get()
            {
                return getSuperCategory();
            }
        };
    }

    @SuppressWarnings("unchecked") public Collection<User> getUsers()
    {
        return (Collection) users.values();
    }

    public Conflict fillConflictDisableInformation(User user, Conflict orig)
    {
        ConflictImpl conflict = (ConflictImpl) orig.clone();
        String id = conflict.getId();
        conflict.setAppointment1Enabled(!disabledConflictApp1.contains(id));
        conflict.setAppointment2Enabled(!disabledConflictApp2.contains(id));
        Date lastChangedInCache = conflictLastChanged.get(id);
        Date origLastChanged = conflict.getLastChanged();

        Date lastChanged = origLastChanged;
        if ( lastChanged == null || (lastChangedInCache != null && lastChangedInCache.after( lastChanged)) )
        {
            lastChanged = lastChangedInCache;
        }
        if ( lastChanged == null)
        {
            lastChanged = new Date();
        }
        conflict.setLastChanged(lastChanged);
        EntityResolver cache = this;
        if (user != null)
        {

            final String reservation1Id = conflict.getReservation1();
            final String reservation2Id = conflict.getReservation2();
            Reservation reservation1 = tryResolve( reservation1Id,Reservation.class);
            Reservation reservation2 = tryResolve( reservation2Id, Reservation.class);
            final boolean appointment1Editable = reservation1 != null && permissionController.canModify(reservation1, user);
            conflict.setAppointment1Editable(appointment1Editable);
            final boolean appointment2Editable = reservation2 != null && permissionController.canModify(reservation2, user);
            conflict.setAppointment2Editable(appointment2Editable);
        }
        return conflict;
    }

    @SuppressWarnings("unchecked") public Collection<String> getConflictIds()
    {
        final HashSet result = new HashSet(disabledConflictApp1);
        result.addAll(disabledConflictApp2);
        return result;
    }

    @SuppressWarnings("unchecked") public Collection<Allocatable> getAllocatables()
    {
        return (Collection) resources.values();
    }

    @SuppressWarnings("unchecked") public Collection<Reservation> getReservations()
    {
        return (Collection) reservations.values();
    }

    @SuppressWarnings("unchecked") public Collection<DynamicType> getDynamicTypes()
    {
        return (Collection) dynamicTypes.values();
    }


    public Collection<Conflict> getDisabledConflicts()
    {
        List<Conflict> disabled = new ArrayList<Conflict>();
        for (String conflictId : getConflictIds())
        {
            Date lastChanged = conflictLastChanged.get( conflictId);
            if ( lastChanged == null)
            {
                lastChanged = new Date();
            }
            Conflict conflict = new ConflictImpl( conflictId, lastChanged, lastChanged);
            Conflict conflictClone = fillConflictDisableInformation( null, conflict);
            disabled.add(conflictClone);
        }
        return disabled;
    }

}
