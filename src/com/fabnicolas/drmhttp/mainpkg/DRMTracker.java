package com.fabnicolas.drmhttp.mainpkg;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;


public class DRMTracker implements Runnable{
  private final static String os_user=System.getProperty("user.name");
  protected String serial;
  protected String uuid;
  protected String host;
  
  /*
  * DRMTracker constructor. Serial and UUID number are generated depending on the operative system you're using
  * (at the moment only Windows O.S. through fetching wmic output).
  *
  * Host must be passed as parameter to identify web server files necessary for authenticating the user.
  *
  * @params host	Full URL of the web service who holds authentication details.
  */
  public DRMTracker(String host) throws IOException{
    if(System.getProperty("os.name").toLowerCase().contains("win")){
      Process process = Runtime.getRuntime().exec(new String[]{"wmic", "bios", "get", "serialnumber"});
      process.getOutputStream().close();
      Scanner sc = new Scanner(process.getInputStream());
      sc.next();
      this.serial = sc.next();
      sc.close();
      process = Runtime.getRuntime().exec(new String[]{"wmic", "csproduct", "get", "UUID"});
      process.getOutputStream().close();
      sc = new Scanner(process.getInputStream());
      sc.next();
      this.uuid = sc.next();
      sc.close();
    }
    this.host = host;
    this.preDRM();
  }
  
  /*
  * DRMTracker secondary constructor.
  * Through this constructor it is possible to pass your own authentication data (serial and UUID).
  * Notice that you should pass only unique couple of data in order to avoid ambiguity between multiple machines and you should provide very hard hackable data.
  *
  * Host must be passed as parameter to identify web server files necessary for authenticating the user.
  *
  * @params serial First authentication parameter, generally a machine number.
  * @params uuid Second authentication parameter, generally an user ID, or an user name, as univoque as possible.
  * @params host	Full URL of the web service who holds authentication details.
  */
  public DRMTracker(String serial,String uuid,String host){
    this.serial=serial;
    this.uuid=uuid;
    this.host=host;
    this.preDRM();
  }
  
  /*
  * Pre-DRM phase. This method is empty and can be overriden in order do achieve something before the actual
  * verification phase, for example retrieving fields 'serial' and 'uuid' and store them somewhere else.
  */
  protected void preDRM(){}
  
  @Override
  public void run(){
    try{
      postDRM(verify());
    }catch(ParseException | IOException e){
      postDRM("FAIL");
    }
  }
  
  /*
  * Post-DRM phase. This method can be overriden in order do achieve something after verification phase;
  * this method is invoked just after it and it is responsible for handling status message from the web
  * authentication service you've given as 'host' parameter.
  *
  * The default implementation checks if 'status' is equal to "Access OK"; if it is false, it closes the
  * whole program using System.exit(0).
  *
  * You can again change this behavior just by overriding the method.
  *
  * @param status	Status given from the web service for verification phase.
  */
  protected void postDRM(String status){
    if(!status.equals("Access OK")) System.exit(0);
  }
  
  /*
  * Verification phase.
  * This method sends to the 'host' specified a request with serial, UUID and current user of the operative system
  * this program is running into.
  *
  * @return	"Access OK" in case of success, an error message from the webserver in case of errors, "FAIL" in case of authentication failed.
  */
  protected final String verify() throws ParseException, IOException{
    HttpClient httpclient = HttpClientBuilder.create().build();
    HttpPost httppost = new HttpPost(this.host+"screenshots/vercert.php?serial="+serial+"&uuid="+uuid+"&useraid="+os_user);
    httpclient.execute(httppost);
    HttpResponse response = httpclient.execute(httppost);
    HttpEntity resEntity = response.getEntity();
    
    if(resEntity != null){
      return EntityUtils.toString(resEntity);
    }else{
      return "FAIL";
    }
  }
  
  /*
  * Upload method. This function calls upload(BufferedImage screen) method.
  * The difference between this method and that one is that this method also captures a screenshot
  * from GPU machine using AWT Robot class and GraphicsEnvironment to retrieve screen dimensions.
  *
  * The screenshot captured will be sent on web-server, encrypted using +5-buffer algorithm and GZip.
  */
  public void uploadScreen() throws AWTException, IOException{
    GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    int width = gd.getDisplayMode().getWidth();
    int height = gd.getDisplayMode().getHeight();
    Robot r = new Robot();
    this.upload(r.createScreenCapture(new Rectangle(0,0,width,height)));
  }
  
  /*
  * Upload method. This method takes a given BufferedImage screenshot and sends it on web server,
  * encrypted using +5-buffer algorithm and GZip.
  *
  * You can capture screen with any method. For easier way, use uploadScreen() method which provides
  * a screenshot through AWT Robot and uploads it using this function.
  *
  * @param screen	The screen captured.
  */
  public void upload(BufferedImage screen) throws IOException{
    String filename = "sc_"+uuid+"_"+os_user+".tmp";
    String fullpath=System.getProperty("user.home")+"\\"+filename;
    ImageIO.write(screen, "png", new File(fullpath+"CACHE"));
    DRMTracker.gzipIt(fullpath+"CACHE", fullpath);
    
    HttpClient httpclient = HttpClientBuilder.create().build();
    
    HttpPost httppost = new HttpPost(this.host+"screenshots/refl.php"); // Upload encrypted screenshot for security reasons.
    File file = new File(fullpath);
    
    MultipartEntityBuilder mpEntity = MultipartEntityBuilder.create();
    ContentBody cbFile = new FileBody(file);
    mpEntity.addPart("userfile", cbFile);
    
    
    httppost.setEntity(mpEntity.build());
    /*HttpResponse response =*/ httpclient.execute(httppost);
    
    /*
    int code = response.getStatusLine().getStatusCode();
    System.out.print("STATUS: "+code+", RESPONSE=");
    BufferedInputStream bis = new BufferedInputStream(response.getEntity().getContent());
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    int result = bis.read();
    while(result != -1) {
        buf.write((byte) result);
        result = bis.read();
    }
    System.out.println(buf.toString("UTF-8"));*/
    
    new File(fullpath+"CACHE").delete();
    new File(fullpath).delete();
  }
  
  /*
  * Encryption & Compression method.
  * This method, used from upload(BufferedImage screen) method, encrypts an image using +5-buffer algorithm
  * and GZips it to save space and provide more security.
  *
  * @param	input The input file path.
  * @param output	The output file path.
  */
  private static void gzipIt(String input,String output){
    byte[] buffer = new byte[1024];
    
    try{
      GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(output)){{def.setLevel(Deflater.BEST_COMPRESSION);}};
      FileInputStream in = new FileInputStream(input);
      
      int len;
      while((len = in.read(buffer)) > 0){
        for(int i=0;i<1024;i++){
          buffer[i]+=5;
        }
        gzos.write(buffer, 0, len);
      }
      
      in.close();
      gzos.finish();
      gzos.close();
    }catch(IOException ex){
      ex.printStackTrace();
    }
  }
  
}