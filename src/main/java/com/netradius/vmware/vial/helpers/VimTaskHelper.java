package com.netradius.vmware.vial.helpers;

import com.vmware.vim25.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * @author Erik R. Jensen
 */
@Slf4j
public class VimTaskHelper {

	public static PropertyFilterSpec createPropertyFilterSpec(ManagedObjectReference ref, String[] filterProps) {
		PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
		ObjectSpec objectSpec = new ObjectSpec();
		objectSpec.setObj(ref);
		objectSpec.setSkip(Boolean.FALSE);
		propertyFilterSpec.getObjectSet().add(objectSpec);
		PropertySpec propertySpec = new PropertySpec();
		propertySpec.getPathSet().addAll(Arrays.asList(filterProps));
		propertySpec.setType(ref.getType());
		propertyFilterSpec.getPropSet().add(propertySpec);
		return propertyFilterSpec;
	}

	public static Object[] wait(VimPortType vimPort, ServiceContent serviceContent, ManagedObjectReference taskRef,
			String[] filterProps, String endWaitProp, Object[] endWaitVals)
			throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {
		return wait(vimPort, serviceContent, taskRef, filterProps, new String[] {endWaitProp}, new Object[][] {endWaitVals});
	}

	public static Object[] wait(VimPortType vimPort, ServiceContent serviceContent, ManagedObjectReference taskRef,
			String[] filterProps, String[] endWaitProps, Object[][] endWaitVals)
			throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {

		PropertyFilterSpec spec = createPropertyFilterSpec(taskRef, filterProps);
		ManagedObjectReference filterRef = vimPort.createFilter(serviceContent.getPropertyCollector(), spec, true);

		Object[] currEndWaitVals = new Object[endWaitProps.length];
		Object[] currFilterVals = new Object[filterProps.length];
		boolean reached = false;
		String version = "";
		do {
			UpdateSet updateSet = vimPort.waitForUpdatesEx(serviceContent.getPropertyCollector(), version, new WaitOptions());
			if (updateSet != null && updateSet.getFilterSet() != null) {
				version = updateSet.getVersion();
				for (PropertyFilterUpdate propertyFilterUpdate : updateSet.getFilterSet()) {
					for (ObjectUpdate objectUpdate : propertyFilterUpdate.getObjectSet()) {
						for (PropertyChange propertyChange : objectUpdate.getChangeSet()) {
							// Get the filter property values
							for (int i = 0; i < filterProps.length; i++) {
								if (propertyChange.getName().contains(filterProps[i])) {
									currFilterVals[i] = propertyChange.getOp() == PropertyChangeOp.REMOVE
											? "" : propertyChange.getVal();
								}
							}
							// Get the end wait property values
							for (int i = 0; i < endWaitProps.length; i++) {
								if (propertyChange.getName().contains(endWaitProps[i])) {
									currEndWaitVals[i] = propertyChange.getOp() == PropertyChangeOp.REMOVE
											? "" : propertyChange.getVal();
								}
							}
						}
					}
				}
				// TODO Allow callbacks for filterProperties so we can get updated status
				for (int i = 0; i < filterProps.length; i++) {
					log.trace("Filter Property [" + filterProps[i] + "] has value [" + currFilterVals[i] + "]");
				}

				for (int i = 0; i < endWaitVals.length && !reached; i++) {
					for (int j = 0; j < endWaitVals[i].length && !reached; j++) {
						reached = endWaitVals[i][j].equals(currEndWaitVals[i]);
					}
				}
			}
		} while(!reached);

		vimPort.destroyPropertyFilter(filterRef);
		return currFilterVals;
	}

	public static boolean getTaskResultAfterDone(VimPortType vimPort, ServiceContent serviceContent, ManagedObjectReference task)
			throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {

		boolean retVal = false;

		// info has a property - state for state of the task
		Object[] result = wait(vimPort, serviceContent, task, new String[] {"info.state", "info.error"},
				new String[] {"state"}, new Object[][] {new Object[]{TaskInfoState.SUCCESS, TaskInfoState.ERROR}});

		if (result[0].equals(TaskInfoState.SUCCESS)) {
			retVal = true;
		}
		if (result[1] instanceof LocalizedMethodFault) {
			throw new RuntimeException(((LocalizedMethodFault) result[1]).getLocalizedMessage());
		}
		return retVal;
	}

}
