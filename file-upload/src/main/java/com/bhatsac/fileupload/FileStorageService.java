package com.bhatsac.fileupload;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    @Autowired
    FileStorageProperties fileStorageProperties;
    
    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        // Normalize file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if(fileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) {
                return resource;
            } else {
                throw new MyFileNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("File not found " + fileName, ex);
        }
    }

    
	public Map<String,URL> listAllfiles(HttpServletRequest request) {
		 Map<String,URL> filesMap =new HashMap<String,URL>();
		 try {
	           // Path filePath = this.fileStorageLocation.normalize();
	            Files.list(Paths.get(fileStorageLocation.toString()))
	            .forEach(s->{
	            	try {
						filesMap.put(s.toFile().getName(),
								new URL(request.getScheme(),
										request.getServerName(),
										request.getServerPort(),
										request.getContextPath().concat("/downloadFile/"+s.toFile().getName())));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	            });
	            
	          }catch(IOException ex){
	        	  
	          }
		return filesMap;
	}
	
	
	public Map<String,URL> searchFiles(HttpServletRequest request, String fileName) {
		 Map<String,URL> filesMap =new HashMap<String,URL>();
		 
		 Predicate<File> filterPredicate= (File s) ->{ 
				return s.getName().contains(fileName);};
			 
			 Stream.of(new File(fileStorageProperties.getUploadDir()).listFiles())
				      .filter(file -> !file.isDirectory())
				      .filter(filterPredicate)
				      .forEach(fName->{
			            
			            		System.out.println("we are here!!!!");
			            		try {
									filesMap.put(fName.getName(),
											new URL(request.getScheme(),
													request.getServerName(),
													request.getServerPort(),
													request.getContextPath().concat("/downloadFile/"+fName.getName())));
								} catch (MalformedURLException e) {
									//logger goes here //TODO
								}
							
			            });
			/*  System.out.println(collect);
			  
			Predicate<Path> filterPredicate= (Path p) ->{ 
				return p.toFile().getName().contains(fileName);};
		
	            Files.list(Paths.get(fileStorageLocation.toString()))
	            .filter(filterPredicate)
	            .forEach(s->{
	            	try {
	            		System.out.println("we are here!!!!");
	            		filesMap.put(s.toFile().getName(),
								new URL(request.getScheme(),
										request.getServerName(),
										request.getServerPort(),
										request.getContextPath().concat("/downloadFile/"+s.toFile().getName())));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	            });*/
	            
	          
		return filesMap;
	}
}