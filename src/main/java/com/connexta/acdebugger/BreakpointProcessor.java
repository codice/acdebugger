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
import java.util.PropertyPermission;
import java.util.function.Function;

public class BreakpointProcessor {

  private static final ImmutableList<String> BUNDLE_PERM_WALKER =
      ImmutableList.of("permissions", "bundle", "module", "revisions", "revisions");

  private static final ImmutableList<String> BUNDLE_PROTDOMAIN_WALKER =
      ImmutableList.of("bundle", "module", "revisions", "revisions");

  private static final ImmutableList<String> BLUEPRINT_PERM_WALKER =
      ImmutableList.of("bundleContext", "bundle", "module", "revisions", "revisions");

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
      if (context != null
          && (hasBundlePerms(context)
              || hasBundleProtectionDomain(context)
              || hasBlueprintProtectionDomain(context))) {
        missingPerms.put(getBundleLocation(context), getPermissionString(threadRef));
      }
    }

    if (missingPerms.isEmpty()) {
      System.out.println("this is wrong");
    }
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
    sb.append(", \"");

    // Special case for file permissions for now to manually extract actions values
    switch (obj.name()) {
      case "java.io.FilePermission":
        extractFilePermActions(permission, sb);
        break;
      case "java.util.PropertyPermission":
        extractPropertyPermActions(permission, sb);
        break;
      default:
        // TODO RAP 18 Dec 17: Either invoke getActions() to get value or duplicate the logic of
        // ALL permissions that are known a priori to generate their actions values.
        // E.g. FilePermission maintains an internal bitmask of permissions that is processed
        // on demand to get the actions string
        sb.append("ACTIONS UNKNOWN");
        break;
    }

    sb.append("\"");
    return sb.toString();
  }

  private void extractFilePermActions(ObjectReference permission, StringBuilder sb) {
    extractMaskPermActionType(permission, FilePermission.class, sb);
  }

  private void extractPropertyPermActions(ObjectReference permission, StringBuilder sb) {
    extractMaskPermActionType(permission, PropertyPermission.class, sb);
  }

  private void extractMaskPermActionType(
      ObjectReference permission, Class permClass, StringBuilder sb) {
    IntegerValue value =
        (IntegerValue) permission.getValue(permission.referenceType().fieldByName("mask"));
    Integer mask = value.value();

    try {
      Method meth = permClass.getDeclaredMethod("getActions", int.class);
      meth.setAccessible(true);
      String actions = (String) meth.invoke(null, mask);

      sb.append(actions);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      sb.append("ACTIONS UNKNOWN / ");
      sb.append(permClass);
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
    List<String> walker;
    if (hasBundlePerms(permissions)) {
      walker = BUNDLE_PERM_WALKER;
    } else if (hasBlueprintProtectionDomain(permissions)) {
      walker = BLUEPRINT_PERM_WALKER;
    } else {
      walker = BUNDLE_PROTDOMAIN_WALKER;
    }

    ObjectReference revList = getReference(permissions, walker);
    ArrayReference revArray =
        (ArrayReference) revList.getValue(revList.referenceType().fieldByName("elementData"));
    ObjectReference moduleRev = (ObjectReference) revArray.getValue(0);
    return getValue(
        moduleRev,
        ImmutableList.of("symbolicName"),
        currRef -> ((StringReference) currRef).value());
  }

  private static boolean hasBundlePerms(ObjectReference context) {
    String permissionType =
        getValue(
            context,
            ImmutableList.of("permissions"),
            currRef -> (currRef != null) ? currRef.type().name() : "");

    return permissionType.endsWith("BundlePermissions");
  }

  private static boolean hasBlueprintProtectionDomain(ObjectReference context) {
    return context
        .referenceType()
        .allFields()
        .stream()
        .map(TypeComponent::name)
        .anyMatch("bundleContext"::equals);
  }

  private static boolean hasBundleProtectionDomain(ObjectReference context) {
    return context
        .referenceType()
        .allFields()
        .stream()
        .map(TypeComponent::name)
        .anyMatch("bundle"::equals);
  }

  private static ObjectReference getReference(ObjectReference input, List<String> fieldPath) {
    ObjectReference currRef = input;
    for (String fieldName : fieldPath) {
      currRef = (ObjectReference) currRef.getValue(currRef.referenceType().fieldByName(fieldName));
    }
    return currRef;
  }

  private static <T> T getValue(
      ObjectReference input, List<String> fieldPath, Function<ObjectReference, T> valueFunc) {
    ObjectReference currRef = getReference(input, fieldPath);
    return valueFunc.apply(currRef);
  }
}
