package com.connexta.acdebugger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.SortedSetMultimap;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.TypeComponent;
import com.sun.jdi.event.BreakpointEvent;
import java.io.FilePermission;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

public class BreakpointProcessor {

  private static final ImmutableList<String> BUNDLE_PERM_WALKER =
      ImmutableList.of("permissions", "bundle", "module", "revisions", "revisions");

  private static final ImmutableList<String> BUNDLE_PROTDOMAIN_WALKER =
      ImmutableList.of("bundle", "module", "revisions", "revisions");

  private final SortedSetMultimap<String, String> missingPerms;

  public BreakpointProcessor(SortedSetMultimap<String, String> missingPerms) {
    this.missingPerms = missingPerms;
  }

  void processBreakpoint(Field contextField, BreakpointEvent evt) throws Exception {
    ThreadReference threadRef = evt.thread();
    checkConditions(threadRef, contextField);
  }

  private void checkConditions(ThreadReference threadRef, Field contextField) throws Exception {
    ArrayReference contextArray = getContextArray(contextField, threadRef);
    int size = contextArray.getValues().size();

    for (int i = 0; i < size; i++) {
      ObjectReference context = (ObjectReference) contextArray.getValues().get(i);
      if (context != null && (hasBundlePerms(context) || hasBundleProtectionDomain(context))) {
        missingPerms.put(getBundleLocation(context), getPermissionString(threadRef));
      }
    }
  }

  private boolean hasBundlePerms(ObjectReference context) {
    String permissionType =
        getValue(
            context,
            ImmutableList.of("permissions"),
            currRef -> (currRef != null) ? currRef.type().name() : "");

    return permissionType.endsWith("BundlePermissions");
  }

  private boolean hasBundleProtectionDomain(ObjectReference context) {
    return context
        .referenceType()
        .allFields()
        .stream()
        .map(TypeComponent::name)
        .anyMatch("bundle"::equals);
  }

  private String getPermissionString(ThreadReference threadRef) throws Exception {
    ObjectReference permission = getArgumentReference(threadRef);
    StringBuilder sb = new StringBuilder();
    ClassType obj = (ClassType) permission.referenceType().classObject().reflectedType();
    sb.append(obj.name());
    sb.append(" ");
    StringReference name =
        (StringReference) permission.getValue(permission.referenceType().fieldByName("name"));
    sb.append(name);
    sb.append(" ");

    // Special case for file permissions for now to manually extract actions values
    if (obj.name().equals("java.io.FilePermission")) {
      extractFilePermActions(permission, sb);
    } else {
      // TODO RAP 18 Dec 17: Either invoke getActions() to get value or duplicate the logic of
      // ALL permissions that are known a priori to generate their actions values.
      // E.g. FilePermission maintains an internal bitmask of permissions that is processed
      // on demand to get the actions string
      sb.append("ACTIONS UNKNOWN");
    }

    return sb.toString();
  }

  private void extractFilePermActions(ObjectReference permission, StringBuilder sb) {
    IntegerValue value =
        (IntegerValue) permission.getValue(permission.referenceType().fieldByName("mask"));
    Integer mask = value.value();

    try {
      Method meth = FilePermission.class.getDeclaredMethod("getActions", int.class);
      meth.setAccessible(true);
      String actions = (String) meth.invoke(null, mask);

      sb.append(actions);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      sb.append("FILEPERMS ACTIONS UNKNOWN");
    }
  }

  private ObjectReference getArgumentReference(ThreadReference threadRef)
      throws IncompatibleThreadStateException {
    return (ObjectReference) threadRef.frame(0).getArgumentValues().get(0);
  }

  private ArrayReference getContextArray(Field contextField, ThreadReference threadRef)
      throws IncompatibleThreadStateException {
    return (ArrayReference) threadRef.frame(0).thisObject().getValue(contextField);
  }

  private String getBundleLocation(ObjectReference permissions) {
    List<String> walker =
        hasBundlePerms(permissions) ? BUNDLE_PERM_WALKER : BUNDLE_PROTDOMAIN_WALKER;

    ObjectReference revList = getReference(permissions, walker);
    ArrayReference revArray =
        (ArrayReference) revList.getValue(revList.referenceType().fieldByName("elementData"));
    ObjectReference moduleRev = (ObjectReference) revArray.getValue(0);
    return getValue(
        moduleRev,
        ImmutableList.of("symbolicName"),
        currRef -> ((StringReference) currRef).value());
  }

  private ObjectReference getReference(ObjectReference input, List<String> fieldPath) {
    ObjectReference currRef = input;
    for (String fieldName : fieldPath) {
      currRef = (ObjectReference) currRef.getValue(currRef.referenceType().fieldByName(fieldName));
    }
    return currRef;
  }

  private <T> T getValue(
      ObjectReference input, List<String> fieldPath, Function<ObjectReference, T> valueFunc) {
    ObjectReference currRef = getReference(input, fieldPath);
    return valueFunc.apply(currRef);
  }
}
