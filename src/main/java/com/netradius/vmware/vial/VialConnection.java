package com.netradius.vmware.vial;

import com.vmware.vim25.*;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.xml.ws.BindingProvider;
import java.io.Closeable;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;

/**
 * @author Erik R. Jensen
 */
@Slf4j
public class VialConnection implements Closeable {

	protected boolean alwaysTrustSSL;
	protected String url;
	protected String username;
	protected String password;
	protected VimPortType vimPort;
	protected ServiceContent serviceContent;

	public VialConnection(String host, String username, String password) {
		this.username = username;
		this.password = password;
		url = "https://" + host + "/sdk/vimService";
	}

	protected void initInsecureSSL() throws VialException {
		log.debug("Allowing insecure SSL");
		try {
			// Warning: This code is not the safest as it disables SSL certificate validation
			TrustManager trustManager = new TrustAllTrustManager();
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		} catch (GeneralSecurityException x) {
			throw new VialException("Failed to configure SSL Trust Manager", x);
		}
	}

	private Map<String, ManagedObjectReference> getRefsInContainer(ManagedObjectReference container, String refType)
			throws VialException {
		try {
			ManagedObjectReference viewManager = serviceContent.getViewManager();
			ManagedObjectReference containerView = vimPort.createContainerView(
					viewManager, container, Collections.singletonList(refType), true);
			Map<String, ManagedObjectReference> map = new HashMap<>();

			PropertySpec propertySpec = new PropertySpec();
			propertySpec.setAll(Boolean.FALSE);
			propertySpec.setType(refType);
			propertySpec.getPathSet().add("name");

			TraversalSpec traversalSpec = new TraversalSpec();
			traversalSpec.setName("view");
			traversalSpec.setPath("view");
			traversalSpec.setSkip(false);
			traversalSpec.setType("ContainerView");

			ObjectSpec objectSpec = new ObjectSpec();
			objectSpec.setObj(containerView);
			objectSpec.setSkip(Boolean.TRUE);
			objectSpec.getSelectSet().add(traversalSpec);

			PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
			propertyFilterSpec.getPropSet().add(propertySpec);
			propertyFilterSpec.getObjectSet().add(objectSpec);

			List<PropertyFilterSpec> propertyFilterSpecs = new ArrayList<>();
			propertyFilterSpecs.add(propertyFilterSpec);

			RetrieveResult retrieveResult = vimPort.retrievePropertiesEx(serviceContent.getPropertyCollector(),
					propertyFilterSpecs, new RetrieveOptions());

			List<ObjectContent> objectContents = new ArrayList<>();
			if (retrieveResult != null && retrieveResult.getObjects() != null
					&& !retrieveResult.getObjects().isEmpty()) {
				objectContents.addAll(retrieveResult.getObjects());
			}

			String token = null;
			if (retrieveResult != null && retrieveResult.getToken() != null) {
				token = retrieveResult.getToken();
			}
			while (token != null && !token.isEmpty()) {
				retrieveResult = vimPort.continueRetrievePropertiesEx(serviceContent.getPropertyCollector(), token);
				token = null;
				if (retrieveResult != null) {
					token = retrieveResult.getToken();
					if (retrieveResult.getObjects() != null && !retrieveResult.getObjects().isEmpty()) {
						objectContents.addAll(retrieveResult.getObjects());
					}
				}
			}
			for (ObjectContent objectContent : objectContents) {
				ManagedObjectReference ref = objectContent.getObj();
				String name = null;
				List<DynamicProperty> properties = objectContent.getPropSet();
				if (properties != null) {
					for (DynamicProperty property : properties) {
						name = (String)property.getVal();
					}
					map.put(name, ref);
				}
			}
			return map;
		} catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg r) {
			throw new VialException(r);
		}
	}

//	protected boolean getTaskResultAfterDone(ManagedObjectReference task) {
//
//		boolean retVal = false;
//		// info has a property - state for state of the task
//		Object[] result =
//				waitForValues.wait(task, new String[]{"info.state", "info.error"},
//						new String[]{"state"}, new Object[][]{new Object[]{
//								TaskInfoState.SUCCESS, TaskInfoState.ERROR}});
//
//		if (result[0].equals(TaskInfoState.SUCCESS)) {
//			retVal = true;
//		}
//		if (result[1] instanceof LocalizedMethodFault) {
//			throw new RuntimeException(
//					((LocalizedMethodFault) result[1]).getLocalizedMessage());
//		}
//		return retVal;
//	}

	/**
	 * Opens a new connection and authenticates.
	 *
	 * @throws VialException
	 */
	public void open() throws VialException {
		log.debug("Opening connection");
		if (alwaysTrustSSL) {
			initInsecureSSL();
		}
		ManagedObjectReference servicesInstance = new ManagedObjectReference();
		servicesInstance.setType("ServiceInstance");
		servicesInstance.setValue("ServiceInstance");
		VimService vimService = new VimService();
		vimPort = vimService.getVimPort();
		Map<String, Object> ctx = ((BindingProvider) vimPort).getRequestContext();
		ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
		ctx.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
		try {
			serviceContent = vimPort.retrieveServiceContent(servicesInstance);
			vimPort.login(serviceContent.getSessionManager(), username, password, null);
			log.debug("Connection opened successfully");
		} catch (InvalidLoginFaultMsg ix) {
			throw new VialException("Invalid username or password", ix);
		} catch (RuntimeFaultFaultMsg | InvalidLocaleFaultMsg x) {
			throw new VialException("Failed to open connection", x);
		}
	}

	/**
	 * Closes the connection if it was open
	 */
	public void close() {
		log.debug("Closing connection");
		if (vimPort != null && serviceContent != null) {
			try {
				vimPort.logout(serviceContent.getSessionManager());
			} catch (RuntimeFaultFaultMsg x) {
				log.error("An error occurred while logging out", x);
			}
		}
		vimPort = null;
		serviceContent = null;
	}


	/**
	 * When set to true, SSL certification validation will be disabled when the login method is executed.
	 * Please be aware this is not secure and will affect the entire JVM.
	 *
	 * @param alwaysTrustSSL true to always trust SSL, false if otherwise
	 */
	public void setAlwaysTrustSSL(boolean alwaysTrustSSL) {
		this.alwaysTrustSSL = alwaysTrustSSL;
	}

	public List<VirtualMachine> getVirtualMachines() throws VialException {
		Map<String, ManagedObjectReference> vmRefs = getRefsInContainer(serviceContent.getRootFolder(), "VirtualMachine");
		List<VirtualMachine> vms = new ArrayList<>(vmRefs.size());
		for (Map.Entry<String, ManagedObjectReference> entry : vmRefs.entrySet()) {
			vms.add(new VirtualMachine(this, entry.getValue(), entry.getKey()));
		}
		return vms;
	}

	// TODO In the future this should be done inside getRefsInContainer
	public VirtualMachine getVirtualMachine(String name) throws VialException {
		List<VirtualMachine> vms = getVirtualMachines();
		for (VirtualMachine vm : vms) {
			if (vm.getName().equals(name)) {
				return vm;
			}
		}
		return null;
	}

}
