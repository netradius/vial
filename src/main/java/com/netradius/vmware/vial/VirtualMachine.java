package com.netradius.vmware.vial;

import com.netradius.vmware.vial.helpers.VimTaskHelper;
import com.vmware.vim25.*;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Erik R. Jensen
 */
@ToString
public class VirtualMachine extends VialObject {

	@Getter
	private String name;

	protected VirtualMachine(VialConnection connection, ManagedObjectReference ref, String name) {
		super(connection, ref);
		this.name = name;
	}

	public void createSnapshot(String name, String description, boolean memory, boolean quiesce)
			throws VialException {
		try {
			ManagedObjectReference taskRef = connection.vimPort.createSnapshotTask(
					ref, name, description, memory, quiesce);
			VimTaskHelper.getTaskResultAfterDone(connection.vimPort, connection.serviceContent, taskRef);
		} catch (FileFaultFaultMsg | InvalidNameFaultMsg | InvalidStateFaultMsg | RuntimeFaultFaultMsg |
				SnapshotFaultFaultMsg | TaskInProgressFaultMsg | VmConfigFaultFaultMsg | InvalidPropertyFaultMsg |
				InvalidCollectorVersionFaultMsg x) {
			throw new VialException(x);
		}
	}

	public void createClone(String cloneName) {
		VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
		VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
//		// TODO set host and data store
		cloneSpec.setLocation(relocateSpec);
		cloneSpec.setPowerOn(false); // TODO
		cloneSpec.setTemplate(false); // TODO
		ManagedObjectReference cloneTask = connection.vimPort.cloneVMTask(ref, folderRef, cloneName, cloneSpec);
	}
}
