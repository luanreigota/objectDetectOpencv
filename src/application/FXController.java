package application;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class FXController {

	@FXML
	private Button startCam;

	@FXML
	private ImageView originalFrame;

	@FXML
	private ImageView frame;

	@FXML
	private Slider hMin;
	
	@FXML
	private Slider hMax;
	
	@FXML
	private Slider sMin;
	
	@FXML
	private Slider sMax;
	
	@FXML
	private Slider vMin;
	
	@FXML
	private Slider vMax;
	
	
	
	private boolean startVideo = false;

	private ScheduledExecutorService timer;

	private VideoCapture capture = new VideoCapture();

	public void startCapture() {

		if (!startVideo) {
			capture.open(0);
			if (capture.isOpened()) {
				System.out.println("Ativando Camera");
				startVideo = true;
				startCam.setText("Stop Cam");

				Runnable frameGrabber = new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						originalFrame.setImage(grabFrame().get(0));
						frame.setImage(grabFrame().get(1));
					}
				};
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
			}
		} else {
			System.out.println("Desativando Camera");
			try {
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// log the exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
			startVideo = false;
			startCam.setText("Start Cam");
			this.capture.release();
		}

	}

	public List<Image> grabFrame() {
		List<Image> imageToShow = new ArrayList<>();
		Mat frame = new Mat();
		Mat originalFrame = new Mat();

		
//		int H_MIN = 0;
//		int H_MAX = 256;
//		int S_MIN = 0;
//		int S_MAX = 256;
//		int V_MIN = 0;
//		int V_MAX = 256;
		if (this.capture.isOpened()) {
			try {
				this.capture.read(originalFrame);
				if (!originalFrame.empty()) {
					//convertendo cor da imagem
					Imgproc.cvtColor(originalFrame, frame, Imgproc.COLOR_BGR2HSV);
					
					//determina range de pixels
					Core.inRange(frame, new Scalar(hMin.getValue(),sMin.getValue(),vMin.getValue()), new Scalar(hMax.getValue(),sMax.getValue(),vMax.getValue()), frame);
					Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3));
					Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8,8));
					
					//aplicando erosão
					Imgproc.erode(frame, frame, erodeElement);
					Imgproc.erode(frame, frame, erodeElement);
					
					//aplicando dilatação
					Imgproc.dilate(frame, frame, dilateElement);
					Imgproc.dilate(frame, frame, dilateElement);
					
//					hmin: 145.2702702702703
//					smin: 70.94594594594595
//					vmin: 141.89189189189187
//					hmax: 317.56756756756755
//					smix: 148.64864864864862
//					vmix: 334.4594594594595

					//detecta range de pixel e desenha um uma linha ao redor
					trackFilteredObject(frame, originalFrame);
					
					imageToShow.add(mat2Image(originalFrame));
					imageToShow.add(mat2Image(frame));
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		return imageToShow;
	}

	public Image mat2Image(Mat frame) {
		MatOfByte buffer = new MatOfByte();
		Imgcodecs.imencode(".png", frame, buffer);
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}

	public void trackFilteredObject(Mat frame, Mat originalFrame){
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		double x, y = 0;
		Mat hierarchy = new Mat();
		
		Imgproc.findContours(frame, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
		
		System.out.println(contours.size());
		if(contours.size()>0){
			for (int i = 0; i < contours.size(); i++) {
				Moments moments = Imgproc.moments((Mat)contours.get(i));
				double area = moments.m00;

				x = moments.m10/area;
				y = moments.m01/area;
				
				System.out.println("area"+i+": "+area);
				System.out.println("x"+i+": "+x);
				System.out.println("y"+i+": "+y);
				
				Imgproc.putText(originalFrame, "rect", new Point(x-30, y), 2, 1, new Scalar(0, 255, 0));
				Imgproc.drawContours(originalFrame, contours, i, new Scalar(0,255,0), 8);
			}
			Imgproc.putText(originalFrame, "TRACKING OBJECT", new Point(0, 50), 2, 1, new Scalar(0, 255, 0));
		}
	}
	


	
}
