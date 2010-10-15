/**
 * Copyright 2008 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hupo.psi.mi.psiscore.ws;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hupo.psi.mi.psiscore.InvalidArgumentException;
import org.hupo.psi.mi.psiscore.PsiscoreException;
import org.hupo.psi.mi.psiscore.PsiscoreFault;
import org.hupo.psi.mi.psiscore.Report;
import org.hupo.psi.mi.psiscore.ResultSet;
import org.hupo.psi.mi.psiscore.ws.config.PsiscoreServerProperties;

import org.hupo.psi.mi.psiscore.model.PsiscoreInput;

import psidev.psi.mi.xml.model.EntrySet;

/**
 * Stores scoring jobs as temp files on the hard drive
 * These temp files can be deleted once a job has been 
 * retrieved or a certain timespan has pased.
 * @author hagen
 *
 */
public class TempFileDataStorage {
	
	private Properties properties = PsiscoreServerProperties.getInstance().getProperties();
	private String writeDir = properties.getProperty("tempDir"); //or use System.getProperty("java.io.tmpdir")
	private final long maxFiles = Long.parseLong(properties.getProperty("maxTempDirJobs")) * 2; 
	private final double deletePercentage = Double.parseDouble(properties.getProperty("deletePercentage"));
	private final long maxSize = Long.parseLong(properties.getProperty("maxTempDirSize")) * 1024 * 1024; // input is megabyte, we sum in kb
	
	private final String fileEndingReports = ".r";
	private final String fileEndingInputData = ".i";
	private final String fileEndingScoringParameters = ".p";
	private final String deletedFilesString = "_deleted_files.gz";
	
	private long numFiles = 0;
	private long dirSize = 0;
	private boolean deleteInProgress = false;
	private Set<String> deletedFiles = null;
	
	
	private static TempFileDataStorage instance = new TempFileDataStorage();
		    
    public static TempFileDataStorage getInstance() {
        return instance;
    }

    private TempFileDataStorage(){
    	
    	try{
    		initDataStorage(true);
    	}catch(PsiscoreException e){
    		e.printStackTrace();
    	}
    	dirSize = getUsedSpace();
    	numFiles = getNumberOfFiles();
    	deletedFiles = getDeletedFiles();
    	System.out.println("Temp directory: " + writeDir + " contains " + numFiles + " files (size: " + dirSize + " bytes)" );
    }
    
    public synchronized PsiscoreInput getInputData(String jobId) throws InvalidArgumentException{
    	PsiscoreInput input = null;
		input = (PsiscoreInput) getObject(jobId + fileEndingInputData);
    	return input;

    }
    
    
    public synchronized Report getReport(String jobId) throws InvalidArgumentException{
    	Report report = null;
		report = (Report) getObject(jobId + fileEndingReports);
    	return report;
    }
    
    
    public ScoringParameters getScoringParameters(String jobId) throws InvalidArgumentException{
    	ScoringParameters scoringParameters = null;
    	scoringParameters = (ScoringParameters) getObject(jobId);
    	return scoringParameters;
    }
    
    public synchronized void storeInputData(String jobId, PsiscoreInput input) throws PsiscoreException{
    	if (dirSize > maxSize || numFiles > maxFiles){
			deleteOldFiles();
		}
    	storeObject(jobId + fileEndingInputData,  input);
    }
    
    public synchronized void storeScoringParameters(String jobId, ScoringParameters params) throws PsiscoreException{
    	if (dirSize > maxSize || numFiles > maxFiles){
			deleteOldFiles();
		}
    	storeObject(jobId + fileEndingScoringParameters,  params);
    }

    
    public synchronized void storeReport(String jobId, Report report){
    	/*if (dirSize > maxSize || numFiles > maxFiles){
			deleteOldFiles();
		}*/
    	try {
			storeObject(jobId + fileEndingReports, report);
		} catch (PsiscoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public synchronized void storeDeletedFiles(Set<String> deletedFiles){
    	//storeObject(deletedFilesString, deletedFiles);
    }
    
    public synchronized void deleteStoredInputData(String jobId){
    	deleteObject(jobId + fileEndingInputData);
    	deleteObject(jobId + fileEndingReports);
     }
    
    public synchronized long getUsedSpace() {
    	if (!deleteInProgress){
    		dirSize = getDirectorySize(new File(writeDir));
    	}
    	return dirSize;
    }
    

    public synchronized long getNumberOfFiles() {
    	if (!deleteInProgress){
    		numFiles = getFileCount(new File(writeDir));
    	}
    	return numFiles;
    }
    
    public synchronized Set<String> getDeletedFiles(){
    	//try{
    		//deletedFiles = (Set<String>) getObject(deletedFilesString);
    	//}catch(InvalidArgumentException e){
    		//new File(writeDir + deletedFilesString);
    	//}
    	if (deletedFiles == null){
    		deletedFiles = new HashSet<String>();
    	}
    	return deletedFiles;
    }
    
    /**
     * Return a list of all the ids that are currently stored as filed in FIFO 
     * order. This means that the oldest ids, those that have been stored in the 
     * temp dir the longest, are the first elements in the list. The youngest files
     * are at the end of the list.
     * @return a list of ids in FIFO order 
     */
    public synchronized List<String> getIdsFIFO(){
    	List<String> ids = new ArrayList<String>();
    	File[] files = getSortedDirListFIFO();
    	for (File file:files){
    		ids.add(file.getName());
    	}
    	return ids;
    	
    }

    public boolean changeWriteDir(String pathWrite, boolean deleteContent) throws PsiscoreException{
    	if (pathWrite != null){
    		writeDir = pathWrite;
    	}
    	initDataStorage(deleteContent);
    	return true;
    }
	    
    /**
     * Initialize the data storage. If data should be kept in a database, this is
     * where the conenction could be made or if data is kept in memory, this is 
     * where the objects could be created.
     */
    private synchronized void initDataStorage(boolean deleteContent) throws PsiscoreException{
    	// clear all the old content or create the directoy if it doesnt exist
    	System.out.println("initializing data storage in " + writeDir);
    	File dir = new File(writeDir);
    	if (!dir.exists()){
    		dir.mkdir();
    	}
    	if (!dir.exists()){
    		throw new PsiscoreException("Cannot create directory specified in properties file. Please make sure it is a valid path");
    	}
    	dir = null;
    	if (!writeDir.endsWith(System.getProperties().getProperty("file.separator"))){
    		writeDir += System.getProperties().getProperty("file.separator");
    	}
    	writeDir += "server" + System.getProperties().getProperty("file.separator");
    	dir = new File(writeDir);
    	if (!dir.exists()){
    		dir.mkdir();
    	}
    		
		if (deleteContent){
			deleteDirectoryContent(new File(writeDir));
		}else if (getUsedSpace() > maxSize || getNumberOfFiles() > maxFiles){
			deleteOldFiles();
		}
    	
    	testDataStorage();
    }

    
   private void testDataStorage() throws PsiscoreException{
    	File dir = new File(writeDir);
    	
    	System.out.print("TEST 1: does " + writeDir + " exist: ");
    	if (!dir.exists()){
    		System.out.println(" NO");
    		throw new PsiscoreException("directory " + writeDir + " does still not exist", new PsiscoreFault());
    	}else{
    		System.out.println(" YES");
    	}
    	
    	try {
    		System.out.print("TEST 2: can I write to " + writeDir + ": ");
	    	storeObject("test.file", new String("YES"));
	    	System.out.println("YES");
    	}catch(PsiscoreException e){
    		System.out.println("NO");
    		throw new PsiscoreException("cannot write test file  ", new PsiscoreFault(), e);
    	}
    	
    	try {
    		System.out.print("TEST 3: can i read from " + writeDir + ": ");
			System.out.println(getObject("test.file"));
		} catch (InvalidArgumentException e) {
			System.out.println("NO");
			throw new PsiscoreException("cannot read the test file  ", new PsiscoreFault(), e);
		}
		
		//System.out.print("TEST 4: can i delete the file again: ");
		//deleteObject("test.file");
		//System.out.print("YES");
	}
    
    /**
     * delete as many of the oldest files as specified to free some space again 
     */
    private synchronized void deleteOldFiles(){
    	//System.out.println("HAVE: " + numFiles + " files with " +dirSize + " ALLOWED: " + maxFiles + " files with " +maxSize);
    	deletedFiles = getDeletedFiles();
    	File[] files = getSortedDirListFIFO();
    	for (int i =0; i< (files.length * deletePercentage); i++){
    		deletedFiles.add(files[i].getName());
    		deleteFile(files[i]);
    	}
    	storeDeletedFiles(deletedFiles);
    	dirSize = getUsedSpace();
    	numFiles = getNumberOfFiles();
    }

    
    private synchronized void deleteDirectoryContent(File dir){
    	deletedFiles = getDeletedFiles();
    	File[] file = dir.listFiles();
    	
	    for(int i=0; i<file.length; i++) {
	         if(file[i].isDirectory()) {
	        	 deleteDirectoryContent(file[i]);
	         }
	         else {
	        	 if (!file[i].getName().equals(deletedFilesString)){
	        	 	 deletedFiles.add(file[i].getName());
	        		 file[i].delete();
	        	 }
	         }
	      }
	    storeDeletedFiles(deletedFiles);
	    numFiles = 0;
	    dirSize = 0;
    }
    
	
    
    
    private synchronized Object getObject(String filename) throws InvalidArgumentException{
    	Object object = null;
    	GZIPInputStream gs = null;
        FileInputStream fis = null;
    	ObjectInputStream ois = null; 
		try {
			//System.out.println(writeDir + filename);
			fis = new FileInputStream(writeDir + filename);
			gs = new GZIPInputStream(fis);
	        ois = new ObjectInputStream(gs);
	        object = ois.readObject();
	        ois.close();
	        fis.close();
	        ois = null;
	        fis = null;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			throw new InvalidArgumentException("There is no file with id " + filename, new PsiscoreFault());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new InvalidArgumentException("Server error trying to access the file " + writeDir + filename, new PsiscoreFault());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new InvalidArgumentException("Server error trying to read from the file system", new PsiscoreFault());
		}
        
    	return object;

    }
    
    
    
    
    /**
     * Store the input data in a database, in local memory, etc ...
     * @param jobId
     * @param input
     */
    private synchronized void storeObject(String filename, Object object) throws PsiscoreException{
    	FileOutputStream fos = null;
    	GZIPOutputStream gz = null;
    	ObjectOutputStream out = null;
    	try
    	{
    		fos = new FileOutputStream(writeDir + filename);
    		
    		gz = new GZIPOutputStream(fos);
    		out = new ObjectOutputStream(gz);
    		out.writeObject(object);
    		out.flush();
    		out.close();
    		fos.close();
    		File tmp = new File(writeDir + filename);
    		if (!tmp.exists()){
    			throw new PsiscoreException("The file " + writeDir + filename + " does not exist, though it should!", new PsiscoreFault());
    		}
    		numFiles++;
    		dirSize += tmp.length();
    		tmp = null;
    	}
    	catch(IOException ex){
    		ex.printStackTrace();
    		
    		throw new PsiscoreException("Cannot store the object to " + filename , new PsiscoreFault(), ex);
    	}
    	
    	fos = null;
    	gz = null;
    	out = null;
    }

    
    private synchronized void deleteObject(String filename){
    	File file = null;
    	deleteInProgress = true;
    	file = new File(writeDir + filename);
    	deleteFile(file);
    	file = null;
    	deleteInProgress = false;
    	
    }
    
    private synchronized void deleteFile(File file){
    	dirSize -= file.length();
    	numFiles--;
		file.delete();
		file = null;
    }
    
    private synchronized long getDirectorySize(File dir) {
    	long size = 0;
    	try{
	    	if (dir.isFile()) {
	    		size = dir.length();
	    	} else {
	    		File[] subFiles = dir.listFiles();
	    		for (File file : subFiles) {
	    			if (file.isFile()) {
	    				size += file.length();
	    			} else {
	    				size += this.getDirectorySize(file);
	    			}
	
	    		}	
	    	}
    	}catch(NullPointerException e){
    		return -1;
    	}
    	return size;
    }

    
    private synchronized long getFileCount(File dir) {
    	long count = 0;
    	try{
	    	if (dir.isFile()) {
	    		count++;
	    	} else {
	    		File[] subFiles = dir.listFiles();
	    		for (File file : subFiles) {
	    			if (file.isFile()) {
	    				count++;
	    			} else {
	    				count += this.getFileCount(file);
	    			}
	
	    		}	
	    	}
    	}catch(NullPointerException e){
    		return -1;
    	}
    	return count;
    }
    
    @SuppressWarnings("unchecked")
	private synchronized File[] getSortedDirListFIFO() {
        File files[] = new File(writeDir).listFiles();
        Arrays.sort( files, new Comparator(){
          public int compare(final Object a, final Object b) {
            return new Long(((File)a).lastModified()).compareTo
                 (new Long(((File) b).lastModified()));
          }
        }); 
        return files;
      }  

    
    
    
    public static void main (String[] args) throws InvalidArgumentException, PsiscoreException{
    	TempFileDataStorage.getInstance();
    	TempFileDataStorage.getInstance().storeObject("tst", new EntrySet());
    	//System.out.println(TempFileDataStorage.getInstance().test("ftp://ftp.no.embnet.org/irefindex/data/current/psimi_tab/10116.mitab.05182010.txt.zip"));
    	//TempFileDataStorage.getInstance().initDataStorage(true);
    	//System.out.println(TempFileDataStorage.getInstance().getIdsFIFO());
    	//TempFileDataStorage.getInstance().storeReport("test", new SerializableReport());
    }
   
}
