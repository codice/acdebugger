package com.connexta.acdebugger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.SortedSetMultimap;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.BreakpointEvent;
import java.util.List;
import java.util.function.Function;

public class BreakpointProcessor {
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
      if (context != null) {
        String permissionType =
            getValue(
                context,
                ImmutableList.of("permissions"),
                currRef -> (currRef != null) ? currRef.type().name() : "");
        if (permissionType.endsWith("BundlePermissions")) {
          missingPerms.put(getBundleLocation(context), getPermissionString(threadRef));
        }
      }
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
    sb.append(" ");

    // TODO RAP 18 Dec 17: Either invoke getActions() to get value or duplicate the logic of
    // ALL permissions that are known a priori to generate their actions values.
    // E.g. FilePermission maintains an internal bitmask of permissions that is processed
    // on demand to get the actions string
    sb.append("ACTIONS UNKNOWN");
    return sb.toString();
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
    return getValue(
        permissions,
        ImmutableList.of("permissions", "bundle", "module", "location"),
        currRef -> ((StringReference) currRef).value());
  }

  private <T> T getValue(
      ObjectReference input, List<String> fieldPath, Function<ObjectReference, T> valueFunc) {
    ObjectReference currRef = input;
    for (String fieldName : fieldPath) {
      currRef = (ObjectReference) currRef.getValue(currRef.referenceType().fieldByName(fieldName));
    }

    return valueFunc.apply(currRef);
  }
}
