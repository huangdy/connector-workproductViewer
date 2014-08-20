package com.saic.uicds.clients.em.workproductViewer;

import gov.ucore.ucore.x20.DigestType;

import java.awt.Desktop;
import java.io.*;
import java.util.zip.*;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Set;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.saic.uicds.clients.em.async.*;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.QNameSet;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFilter;
//import org.jdom.Document;
//import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;
import org.uicds.mapService.GetLayersRequestDocument;
import org.uicds.mapService.GetLayersResponseDocument;
import org.uicds.mapService.SubmitShapefileRequestDocument;
import org.uicds.mapService.SubmitShapefileResponseDocument;
import org.uicds.mapService.GetLayersRequestDocument.GetLayersRequest;
import org.uicds.mapService.SubmitShapefileRequestDocument.SubmitShapefileRequest;
import org.uicds.mapService.SubmitShapefileResponseDocument.SubmitShapefileResponse;
import org.w3c.dom.Node;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import x0.messageStructure1.StructuredPayloadType;
import x0.messageStructure1.impl.StructuredPayloadMetadataDocumentImpl;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.namespace.QName;

//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;

import com.saic.precis.x2009.x06.base.IdentifierType;
import com.saic.precis.x2009.x06.base.ProcessingStateType;
import com.saic.precis.x2009.x06.base.ProcessingStatusType;
import com.saic.precis.x2009.x06.base.ProcessingStateType.Enum;
import com.saic.precis.x2009.x06.payloads.binary.BinaryContentDocument;
import com.saic.precis.x2009.x06.payloads.binary.impl.*;
import com.saic.precis.x2009.x06.payloads.binary.BinaryContentType;
import com.saic.precis.x2009.x06.payloads.link.LinkContentDocument;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;
import com.saic.uicds.clients.util.WebServiceClient;

public class WorkproductAccess {
	
	    Logger log = LoggerFactory.getLogger(this.getClass());

	    private UicdsCore uicdsCore;

	    private WebServiceClient webServiceClient;

	    private String profileID;

	    private String uicdsID;
	    
	    private String connectToCore;

	    private static String INCIDENT_EXAMPLE_FILE = "src/test/resources/workproduct/IncidentSample.xml";
	    
	    private UicdsIncidentManager uicdsIncidentManager;
	    
	    private WorkProduct workProduct;
	    
	    private HashMap<String, UicdsIncident> incidents;
	    
	    private HashMap<String, UicdsWorkProduct> uicdsWPMap;
	    
	    private HashMap<String, String> workproductIncidentMap = new HashMap<String,String>();
	    
	    private HashMap<String, String> wpIdFilenameMap = new HashMap<String,String>();

	    public void setUicdsCore(UicdsCore core) {
	        uicdsCore = core;
	    }

	    public void setWebServiceClient(WebServiceClient client) {
	        webServiceClient = client;
	    }

	    public void setProfileID(String id) {
	        profileID = id;
	    }

	    public void setUicdsID(String id) {
	        uicdsID = id;
	    }
	
	
	public WorkproductAccess() {
		System.out.println("in WorkproductAccess()");
	}
		
	public void init(String[] args) {
		System.out.println("in init()");
		
		// set the uri from the system property
        String targetUri = System.getProperty("target.uri");
        if (targetUri != null) {
            webServiceClient.setURI(targetUri);
        }
        // evaluate command line arguments
        for (int i = 0; i < args.length; i++) {
            // if there's a -u switch then there should be a target uri
        	System.out.println("args["+i+"]="+args[i].toString());
            if (args[i].equals("-u")) {
                i++;
                if (args[i] == null) {
                    System.out.println("Switch -u must be followed with a target URI");
                    return;
                }
                webServiceClient.setURI(args[i]);
                System.out.println("set URI: " + args[i]);
            } else if (args[i].equals("-p")) {
                i++;
                profileID = args[i];
            } else if (args[i].equals("-i")) {
                i++;
                uicdsID = args[i];
            } else {
                usage();
            }
        }
        // Make sure we get a UICDS ID and resource profile ID
        if (profileID == null || uicdsID == null) {
            usage();
            System.exit(0);
        }
        System.out.println("targetUri=" + webServiceClient.getURI() + " profile: " + profileID
            + " UICDS ID: " + uicdsCore.getApplicationID());
        // Initialize a manager for UICDS incidents (Set before
        // initializeUicdsCore so that it
        // can register as a listener for work products because initializing the
        // core will
        // create local incidents for those already on the core.
        
        
          uicdsIncidentManager = new UicdsIncidentManager();
          uicdsIncidentManager.setUicdsCore(uicdsCore);

          // Initialize the connection to the core
          initializeUicdsCore();

          // process notifications to clear out any notifications left over
          processNotifications(2, 1000);

          // Show the current state of the client
          printStartingState(uicdsIncidentManager);
        
	}
	
	
	public void runAsyncClientTest(String incidentID, File file) {
		if (webServiceClient==null)
			System.out.println("webServiceClient is NULL");
		else 
			System.out.println("webServiceClient is NOT NULL");
		
		System.out.println("In WorkproductAccess():incidentID="+incidentID);
		System.out.println("file contents="+file.toString());

        // Create an incident (gets all the associated work products)
        log.info("Creating Incident");
        UicdsIncident uicdsIncident = new UicdsIncident();
        uicdsIncident.setUicdsCore(uicdsCore);

        // Create an IncidentDocument to describe the incident
        IncidentDocument incidentDoc = getIncidentSample();
        if (incidentDoc == null) {
            return;
        }
        
        // Create the incident on the core
        uicdsIncident.createOnCore(incidentDoc.getIncident());

        // Add it to the incident manager
        uicdsIncidentManager.addIncident(uicdsIncident);

        // process notifications to see the incident created
        processNotifications(5, 1000);

        // Get the current incident document
        UICDSIncidentType incidentType = uicdsIncident.getIncidentDocument();
        // Update the incident
        System.out.println("Updating incident: "
            + incidentType.getActivityIdentificationArray(0).getIdentificationIDArray(0).getStringValue());

        // Change the type of incident
        if (incidentType.sizeOfActivityCategoryTextArray() < 1) {
            incidentType.addNewActivityCategoryText();
        }
        incidentType.getActivityCategoryTextArray(0).setStringValue("CHANGED");
        String description = "DEFAULT DESCRIPTION";
        if (incidentType.sizeOfActivityDescriptionTextArray() < 1) {
            incidentType.addNewActivityDescriptionText();
        } else {
            description = incidentType.getActivityDescriptionTextArray(0).getStringValue();
        }
        incidentType.getActivityDescriptionTextArray(0).setStringValue(description + " - ADDITION");

        // Update the incident on the core
        ProcessingStatusType status = uicdsIncident.updateIncident(incidentType);

        
        
        // If the request is pending then process requests until the request is
        // accepted or rejected
        // Get the asynchronous completion token
        if (status != null && status.getStatus() == ProcessingStateType.PENDING) {
            IdentifierType incidentUpdateACT = status.getACT();
            System.out.println("Incident update is PENDING");

            // Process notifications from the core until the update request is
            // completed
            // This loop should also process other incoming notification
            // messages such
            // as updates for other work products
            while (!uicdsCore.requestCompleted(incidentUpdateACT)) {
                // Process messages from the core
                uicdsCore.processNotifications();

                // Get the status of the request we are waiting on
                status = uicdsCore.getRequestStatus(incidentUpdateACT);
            }

            // Check the final status of the request
            status = uicdsCore.getRequestStatus(incidentUpdateACT);
            if (status.getStatus() == ProcessingStateType.REJECTED) {
                log.error("UpdateIncident request was rejected: " + status.getMessage());
            }
        } else if (status == null) {
            System.err.println("Processing status for incident update was null");
        } else {
            System.out.println("Incident update was ACCEPTED");

            System.out.println("Close the incident");
            uicdsIncident.closeIncident(uicdsIncident.getIdentification());
            processNotifications(10, 1000);
        }

        // Dump all the work products for the incident
        // uicdsIncident.dumpWorkProducts();
        int i = 0;
        while (i < 30) {
            i++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Sleep interrupted: " + e.getMessage());
            }
            uicdsCore.processNotifications();
        }

        System.out.println("Archive the incident");
        uicdsIncident.archiveIncident(uicdsIncident.getIdentification());
        processNotifications(10, 1000);
        
        
	}
	
	
	private static void usage() {
        System.out.println("");
        System.out.println("This is the UICDS Asynchronous Example Client.");
        System.out.println("Execution of this client depends on a functioning UICDS server. The default is http://localhost/uicds/core/ws/services");
        System.out.println("To verify that a UICDS server is accessible, use a browser to navigate to http://localhost/uicds/core/ws/services/ResourceProfileService.wsdl\"");
        System.out.println("");
        System.out.println("Usage: java -jar WorkproductViewer.jar [-u <Server URI>] -p <Resource-Profile-ID> -i <UICDS-ID>");
        System.out.println("");
        System.out.println("");
    }
	private void initializeUicdsCore() {
        // The UicdsCore object is created by the Spring Framework as defined in
        // async-context.xml
        // Now set the application specific data for the UicdsCore object

        // Set the UICDS identifier for the application
//        uicdsCore.setApplicationID(uicdsID);

        // Set the site local identifier for this application
        uicdsCore.setLocalID(this.getClass().getName());

        // Set the application profile to use for the connection to the core
//        uicdsCore.setApplicationProfileID(profileID);

        // Set the Web Service Client that will handle web service invocations
        uicdsCore.setWebServiceClient(webServiceClient);

        // Initialize the connection to the core
        if (!uicdsCore.initialize()) {
            log.error("Initialization failed.  Maybe profile does not exist?");
            return;
        }
    }

    private void processNotifications(int count, int milliSeconds) {
        int iterations = -1;
        while (iterations++ < count) {
            processNotifications(milliSeconds);
        }
    }

    private void processNotifications(int milliSeconds) {
        try {
            Thread.sleep(milliSeconds);
        } catch (InterruptedException e) {
            log.error("Sleep interrupted: " + e.getMessage());
        }
        log.info("Processing Notifications ");
        uicdsCore.processNotifications();
    }
    private void printStartingState(UicdsIncidentManager uicdsIncidentManager) {
        log.info("Starting State:");
        HashMap<String, UicdsIncident> incidents = uicdsIncidentManager.getIncidents();
        for (String incidentID : incidents.keySet()) {
            log.info("   Incident ID:   " + incidents.get(incidentID).getIncidentID());
            log.info("   Incident Name: " + incidents.get(incidentID).getName());
            log.info("");
        }
    }
    // look for the sample xml file in a relative location
    private static IncidentDocument getIncidentSample() {
        String incidentExampleFile2 = "../" + INCIDENT_EXAMPLE_FILE;
        try {
            InputStream in = new FileInputStream(INCIDENT_EXAMPLE_FILE);
            IncidentDocument incidentDoc = IncidentDocument.Factory.parse(in);
            return incidentDoc;
        } catch (FileNotFoundException e1) {
            try {
                // try again in case this is being executed from the target
                // directory
                InputStream in = new FileInputStream(incidentExampleFile2);
                IncidentDocument incidentDoc = IncidentDocument.Factory.parse(in);
                return incidentDoc;
            } catch (FileNotFoundException e2) {
                System.err.println("File not found as either of the following paths:");
                System.err.println(INCIDENT_EXAMPLE_FILE);
                System.err.println(incidentExampleFile2);
            } catch (XmlException e2) {
                System.err.println("error parsing files " + " " + e2.getMessage());
            } catch (IOException e2) {
                System.err.println("File IO exception: " + incidentExampleFile2 + " "
                    + e2.getMessage());
            }
        } catch (XmlException e1) {
            System.err.println("error parsing files " + " " + e1.getMessage());
        } catch (IOException e1) {
            System.err.println("File IO exception: " + INCIDENT_EXAMPLE_FILE + " "
                + e1.getMessage());
        }
        return null;
    }
    
	
	public void createIncident(String incidentID, File file) {
		
		System.out.println("In createIncident():incidentID="+incidentID);
		System.out.println("file contents="+file.toString());
		
		// Create an incident (gets all the associated work products)
        log.info("Creating Incident");
        UicdsIncident uicdsIncident = new UicdsIncident();
        uicdsIncident.setUicdsCore(uicdsCore);

        // Create an IncidentDocument to describe the incident
        IncidentDocument incidentDoc = getIncidentSample();
        if (incidentDoc == null) {
            return;
        }

        // Create the incident on the core
        uicdsIncident.createOnCore(incidentDoc.getIncident());

        // Add it to the incident manager
        uicdsIncidentManager.addIncident(uicdsIncident);
        
        

        // process notifications to see the incident created
        processNotifications(5, 1000);
	}
	
    public static final byte[] getPayloadByteArray(StructuredPayloadType payload) {

        XmlObject object = null;
        XmlCursor cursor = payload.newCursor();
        cursor.toFirstChild();
        do {
            try {
                if ((object = XmlObject.Factory.parse(cursor.xmlText())) instanceof StructuredPayloadMetadataDocumentImpl)
                    continue;
                break;
            } catch (XmlException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } while (cursor.toNextSibling());

        cursor.dispose();

        return object != null ? object.toString().getBytes() : null;
    }
	
	public String[][] getListOfIncidents() {
		String dataValues[][];
		System.out.println("in getListOfIncidents()");
		incidents = uicdsIncidentManager.getIncidents();
		int numIncidents = 0;
		numIncidents = incidents.size();
		int numTotal=0;
		
		String[] mimeVals = {"application/msword",
                "Shapefile",
                "application/pdf",
                "text/html"};

	    List<String> mimeList = Arrays.asList(mimeVals);
	    Set<String> acceptedMimeTypes = new HashSet<String>(mimeList);
		
		//Compute size for dataValues
        for (String incidentID : incidents.keySet()) {
        	numTotal++;
        	uicdsWPMap = incidents.get(incidentID).getWorkProductMap();
            int wpSize=0;
            wpSize=uicdsWPMap.size();
            for (String wpID: uicdsWPMap.keySet()) {
            	numTotal++;
            }
        }
		dataValues = new String[numTotal][4];
		
		//System.out.println("keyset=" + incidents.keySet().toString());
		int i=0;
        for (String incidentID : incidents.keySet()) {
                log.info("   Incident ID:   " + incidents.get(incidentID).getIncidentID());
                log.info("   Incident Name: " + incidents.get(incidentID).getName());

                dataValues[i][0]=incidents.get(incidentID).getName();      
                i++;
                //get WPs for each incident
                //incidents.get(incidentID).getAssociatedWorkProducts();
                uicdsWPMap = incidents.get(incidentID).getWorkProductMap();
                int wpSize=0;
                wpSize=uicdsWPMap.size();
                int j=0;
                String filename="";
                for (String wpID: uicdsWPMap.keySet()) {
                	//include only binary and shapefiles
                	String mimeTypeStr="";
                	mimeTypeStr=uicdsWPMap.get(wpID).getWorkProduct().getStructuredPayloadArray(0).getStructuredPayloadMetadata().getCommunityURI();
                	if (acceptedMimeTypes.contains(mimeTypeStr)) {
                	  workproductIncidentMap.put(wpID, incidentID);
                	  dataValues[i][1]=uicdsWPMap.get(wpID).getWorkProduct().getStructuredPayloadArray(0).getStructuredPayloadMetadata().getCommunityURI();
                	
                	  StructuredPayloadType payload = uicdsWPMap.get(wpID).getWorkProduct().getStructuredPayloadArray(0);

                	  byte[] payloadByteArray = getPayloadByteArray(payload);
                      if (payloadByteArray != null) {
                        try {
                          XmlObject payloadContent = XmlObject.Factory.parse(new String(payloadByteArray));
                          if (payloadContent instanceof BinaryContentDocument) {
                            filename = ((BinaryContentDocument) payloadContent).getBinaryContent().getLabel();
                            dataValues[i][2]=filename;
                          } else if (payloadContent instanceof LinkContentDocument) {
                        	filename = ((LinkContentDocument) payloadContent).getLinkContent().getLabel();
                        	dataValues[i][2]=filename;
                          } else {
                        	  // it must be a shapefile
                        	//log.info("payloadContent="+payloadContent);
                        	filename = uicdsWPMap.get(wpID).getWorkProduct().getPackageMetadata().getDataItemID();
                        	log.info("workproduct="+uicdsWPMap.get(wpID).getWorkProduct());
                        	dataValues[i][2]=uicdsWPMap.get(wpID).getWorkProduct().getPackageMetadata().getDataItemID();
                          }
                          
                          wpIdFilenameMap.put(filename, wpID);
                          log.info("label="+filename);
                        } catch (XmlException e) {
                          e.printStackTrace();
                        }
                      } else {
                    	log.info("payloadByteArray is null");
                      }
                      
                	  i++;
                	}
                }
        }
        return dataValues;
	}
	
	public String assocShpToIncidentUsingDOM(String incidentID, File file) {
		String res="";
		System.out.println("In assocShpToIncident():incidentID="+incidentID+". Filename="+file.toString());
		try {
			log.info("the incident you want to attach to is:"+incidents.get(incidentID).getIncidentDocument().toString());
			InputStream in = new FileInputStream(file);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(in, out);
            byte[] bytes = out.toByteArray();
            bytes = Base64.encodeBase64(bytes);
			
            String ns = "http://uicds.org/MapService";
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElementNS(ns, "SubmitShapefileRequest");
            doc.appendChild(root);
            Element incId = doc.createElementNS(ns, "IncidentId");
            root.appendChild(incId);
            incId.setTextContent(incidentID);
            Element cdata = doc.createElementNS(ns, "ContentData");
            root.appendChild(cdata);
            cdata.setTextContent(new String(bytes));
	
            SubmitShapefileRequestDocument ssfrd = SubmitShapefileRequestDocument.Factory.parse(doc);
            log.info(ssfrd.toString());
            
			XmlObject xmlObject = uicdsCore.marshalSendAndReceive(ssfrd);
            SubmitShapefileResponseDocument ssfres = (SubmitShapefileResponseDocument)xmlObject;
			Enum pst = ssfres.getSubmitShapefileResponse().getWorkProductPublicationResponseArray(0).getWorkProductProcessingStatus().getStatus();
			res=pst.toString();
			log.info(xmlObject.xmlText());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}
	
	public String assocShpToIncident(String incidentID, File file) {
		
		System.out.println("In assocShpToIncident():incidentID="+incidentID+". Filename="+file.toString());
		String res="";
		try {
			log.info("the incident you want to attach to is:"+incidents.get(incidentID).getIncidentDocument().toString());
			InputStream in = new FileInputStream(file);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(in, out);
            byte[] bytes = out.toByteArray();
            bytes = Base64.encodeBase64(bytes);
			
			//DigestType dt = DigestType.Factory.newInstance();
			SubmitShapefileRequest ssfr = SubmitShapefileRequest.Factory.newInstance();
			ssfr.setIncidentId(incidentID);
			//ssfr.setDigest(dt);
			
			//ssfr.setContentData(new String(bytes).getBytes());
			ssfr.setContentData(bytes); 

			SubmitShapefileRequestDocument ssrd = SubmitShapefileRequestDocument.Factory.newInstance();
			ssrd.setSubmitShapefileRequest(ssfr);
			
			//log.info("Message="+ssrd.xmlText());
			//log.info("Sending...");
			XmlObject xmlObject = uicdsCore.marshalSendAndReceive(ssrd);
			//log.info("Done.");
			
			SubmitShapefileResponse ssfres = (SubmitShapefileResponse)xmlObject;
			
			Enum pst = ssfres.getWorkProductPublicationResponseArray(0).getWorkProductProcessingStatus().getStatus();
			res=pst.toString();
			//ProcessingStatusType. pst = ssfres.getWorkProductPublicationResponseArray(0).getWorkProductProcessingStatus().getStatus();
			log.info(xmlObject.xmlText());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}
	
	public boolean inflateShz() {
		String zipname = "uicds_temp.zip"; 
		boolean returnVal=false;
		try {            
		  ZipFile zipFile = new ZipFile(new File(zipname));
		  Enumeration enumeration = zipFile.entries(); 
		     
		  while (enumeration.hasMoreElements()) {             
		    ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();           
		    System.out.println("Unzipping: " + zipEntry.getName());           
		    BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(zipEntry)); 
		             
		    int size;              
		    byte[] buffer = new byte[2048]; 
		          
		    FileOutputStream fos = new FileOutputStream(zipEntry.getName());              
		    BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length); 
		             
		    while ((size = bis.read(buffer, 0, buffer.length)) != -1) {                  
		      bos.write(buffer, 0, size);            
		    } 
		 
		    bos.flush(); 
		    bos.close(); 
		    fos.close(); 
		    bis.close();     
		  }
		  returnVal=true;
		  
		} catch (IOException e) {   
		    e.printStackTrace(); 
		}
		return returnVal;
	}
	
	public String findShpfileName(){
		 String fn="";
		 File dir = new File(".");
		 FileFilter fileFilter = new WildcardFilter("*.shp");
		 File[] files = dir.listFiles(fileFilter);
		 for (int i = 0; i < files.length; i++) {
		   System.out.println(files[i]);
		   fn=files[i].getName();
		 }
		 return fn;
	}
	
	public void openTheWorkproduct(String filename) {
		String incidentID="";
		String startMark="<bin:BinaryContent";
		String endMark="</bin:BinaryContent>";
		String linkStartMark="<link:LinkContent";
		String linkEndMark="</link:LinkContent>";
		String encodedData="";
		String structPay="";
		String commURI="";
		String uicdsTempFilename="";
		String shpfileName="";
		boolean wpIsLink=false;
		boolean inflateSuccess = false;
		
		String wpID="";
		wpID = wpIdFilenameMap.get(filename);
		
		incidentID = workproductIncidentMap.get(wpID);
		//log.info("in openTheWorkproduct keyset="+workproductIncidentMap.toString());
		log.info("in openTheWorkproduct incidentID="+incidentID+": "+incidents.get(incidentID).getName());
		uicdsWPMap = incidents.get(incidentID).getWorkProductMap();
		
		//log.info("wp keyset="+uicdsWPMap.keySet());
		//BinaryContentDocument bcd = BinaryContentDocument.Factory.newInstance();
		//WorkProduct wp = uicdsWPMap.get(wpID).getWorkProduct();
		//StructuredPayloadType payload = StructuredPayloadType.Factory.newInstance();
		//payload = wp.getStructuredPayloadArray(0);
		
		
		
		structPay=uicdsWPMap.get(wpID).getWorkProduct().getStructuredPayloadArray(0).toString();
		//log.info("structPay="+structPay);
		
		if (!structPay.contains(startMark) && !structPay.contains(linkStartMark)) {
			startMark="<content ";
			endMark="</content>";
		} else if (structPay.contains(linkStartMark)) {
			startMark=linkStartMark;
			endMark=linkEndMark;
		}
		
		encodedData=structPay.substring(structPay.indexOf(startMark), structPay.indexOf(endMark));
		encodedData=encodedData.substring(encodedData.indexOf(">")+1);
		//log.info("wp="+uicdsWPMap.get(wpID).getWorkProduct().getStructuredPayloadArray(0));
		//log.info("encodedData="+encodedData);
		
		byte[] bytes = Base64.decodeBase64(encodedData.getBytes());
		commURI=uicdsWPMap.get(wpID).getWorkProduct().getStructuredPayloadArray(0).getStructuredPayloadMetadata().getCommunityURI();
		
		log.info("commURI"+commURI);
		
		if (commURI.equalsIgnoreCase("Shapefile")) {
			//uicdsTempFilename = "uicds_temp.shz";
			uicdsTempFilename = "uicds_temp.zip";
		} else if (commURI.equalsIgnoreCase("application/msword")) {
			uicdsTempFilename = "uicds_temp.doc";
		} else if (commURI.equalsIgnoreCase("application/pdf")) {
			uicdsTempFilename = "uicds_temp.pdf";
		} else if (commURI.equalsIgnoreCase("text/html")) {
			wpIsLink=true;
		}
		
		if (wpIsLink) {
			String urlStr="";
			urlStr=uicdsWPMap.get(wpID).getWorkProduct().getStructuredPayloadArray(0).toString();
			String addStrTag="<link:Address>";
			String addStrEndTag="</link:Address>";
			int begAddIdx=0;
			int endAddIdx=0;
			String addressStr="";
			begAddIdx=urlStr.indexOf(addStrTag)+14;
			endAddIdx=urlStr.indexOf(addStrEndTag);
			addressStr=urlStr.substring(begAddIdx, endAddIdx);
			addressStr="http://"+addressStr;
			log.info(urlStr);
			try {   
			      //Process p1 = Runtime.getRuntime().exec("cmd /C \"c:\\Program Files\\Internet Explorer\\iexplore.exe\" "+addressStr+"\"");
			      
				Process p1 = Runtime.getRuntime().exec("\"C:\\Program Files\\Internet Explorer\\IEXPLORE.EXE\" " + addressStr);

	              BufferedReader in = new BufferedReader(   
	                                new InputStreamReader(p1.getInputStream()));   
	              String line = null;   
	              while ((line = in.readLine()) != null) {   
	                System.out.println(line);   
	              }   
	        } catch (IOException e) {   
	            e.printStackTrace();   
		    } catch (Exception e) {
			    e.printStackTrace();
		    }
		} else {
		
		    try {
		
		      OutputStream f1 = new FileOutputStream(uicdsTempFilename); 
		      f1.write(bytes); 
		      f1.close(); 

		      log.info("uicdsTempFilename="+uicdsTempFilename);
		  
		      try {   
			      Process p = Runtime.getRuntime().exec("cmd /C dir");
			      if (commURI.equalsIgnoreCase("application/msword")) {
	                  //p = Runtime.getRuntime().exec("cmd /C \"c:\\Program Files (x86)\\Microsoft Office\\Office14\\WINWORD.EXE\" uicds_temp.doc");
			          Desktop.getDesktop().open(new File("uicds_temp.doc"));
			      } else if (commURI.equalsIgnoreCase("application/pdf")) {
				      //p = Runtime.getRuntime().exec("cmd /C \"c:\\Program Files (x86)\\Adobe\\Reader 9.0\\Reader\\AcroRd32.EXE\" uicds_temp.pdf");
			    	  Desktop.getDesktop().open(new File("uicds_temp.pdf"));
			      } else if (commURI.equalsIgnoreCase("Shapefile")) {
				      //p = Runtime.getRuntime().exec("cmd /C \"c:\\Program Files (x86)\\Adobe\\Reader 9.0\\Reader\\AcroRd32.EXE\" uicds_temp.pdf");
			    	  //uncompress
			    	  
			    	  inflateSuccess = inflateShz();
			    	  shpfileName=findShpfileName();
			    	  Desktop.getDesktop().open(new File(shpfileName));
			      }
		
	              //BufferedReader in = new BufferedReader(   
	                                //new InputStreamReader(p.getInputStream()));   
	              //String line = null;   
	              //while ((line = in.readLine()) != null) {   
	              //  System.out.println(line);   
	              //}   
	          } catch (IOException e) {   
	            e.printStackTrace();   
	          }   
		    } catch (Exception e) {
			    e.printStackTrace();
		    }
		}
	}
	
}
