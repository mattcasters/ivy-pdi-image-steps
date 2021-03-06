package com.ivyis.di.trans.steps.image.resize;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * This class is responsible to processing the data rows.
 * 
 * @author <a href="mailto:joel.latino@ivy-is.co.uk">Joel Latino</a>
 * @since 1.0.0
 */
public class ImageResizeStep extends BaseStep implements StepInterface {

  /** for i18n purposes. **/
  private static final Class<?> PKG = ImageResizeStep.class;
  private ImageResizeStepMeta meta;
  private ImageResizeStepData data;

  public ImageResizeStep(
      StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
    super(s, stepDataInterface, c, t, dis);
  }

  /**
   * {@inheritDoc}
   * 
   * @throws KettleException
   */
  @Override
  public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
    this.meta = (ImageResizeStepMeta) smi;
    this.data = (ImageResizeStepData) sdi;

    final Object[] r = getRow();
    if (r == null) {
      setOutputDone();
      return false;
    }

    if (first) {
      first = false;
      data.outputRowMeta = super.getInputRowMeta();
      data.nrPrevFields = data.outputRowMeta.size();
      meta.getFields(data.outputRowMeta, getStepname(), null, null, this);

      cachePosition();
    } // end if first

    final Object[] outputRow = RowDataUtil.allocateRowData(data.outputRowMeta.size());
    for (int i = 0; i < data.nrPrevFields; i++) {
      outputRow[i] = r[i];
    }

    // Crop image process
    try {
      final RowMetaInterface rowMeta = getInputRowMeta();
      final String fileFromPath = rowMeta.getString(r, data.indexOfFileFromPathField);
      final String fileToPath = rowMeta.getString(r, data.indexOfFileToPathField);
      final int targetWidth = rowMeta.getInteger(r, data.indexOfTargetWidthField).intValue();
      final int targetHeight = rowMeta.getInteger(r, data.indexOfTargetHeightField).intValue();

      final ImagePlus imp = IJ.openImage(fileFromPath);
      final ImageProcessor ip = imp.getProcessor();
      ip.setInterpolate(true);
      final ImageProcessor ip2 = ip.resize(targetWidth, targetHeight);
      final BufferedImage resizedImage = ip2.getBufferedImage();

      ImageIO.write(resizedImage, "jpg", new File(fileToPath));

      if (log.isDetailed()) {
        logDetailed("New File Path: '" + fileToPath + "', Size - Width: " + ip2.getWidth()
            + ", Height: "
            + ip2.getHeight());
      }

    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString(PKG, "ImageResizeStep.Exception.generic"));
    }
    putRow(data.outputRowMeta, outputRow);

    if (checkFeedback(getLinesRead())) {
      if (log.isBasic()) {
        logBasic("Linenr " + getLinesRead()); // Some basic logging
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
    meta = (ImageResizeStepMeta) smi;
    data = (ImageResizeStepData) sdi;

    if (super.init(smi, sdi)) {
      return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
    super.dispose(smi, sdi);
  }

  /**
   * Run is were the action happens.
   */
  public void run() {
    logBasic("Starting to run...");
    try {
      while (processRow(meta, data) && !isStopped()) {
        continue;
      }
    } catch (Exception e) {
      logError("Unexpected error : " + e.toString());
      logError(Const.getStackTracker(e));
      setErrors(1);
      stopAll();
    } finally {
      dispose(meta, data);
      logBasic("Finished, processing " + getLinesRead() + " rows");
      markStop();
    }
  }

  /**
   * Checks the fields positions.
   * 
   * @throws KettleStepException the kettle step exception.
   */
  private void cachePosition() throws KettleStepException {
    if (meta.getFileFromPath() != null && data.indexOfFileFromPathField < 0) {
      data.indexOfFileFromPathField = getInputRowMeta().indexOfValue(meta.getFileFromPath());
      if (data.indexOfFileFromPathField < 0) {
        final String message =
            "Unable to find table name field [" + meta.getFileFromPath() + "] in input row";
        logError(message);
        throw new KettleStepException(message);
      }
    }

    if (meta.getFileToPath() != null && data.indexOfFileToPathField < 0) {
      data.indexOfFileToPathField = getInputRowMeta().indexOfValue(meta.getFileToPath());
      if (data.indexOfFileToPathField < 0) {
        final String message =
            "Unable to find table name field [" + meta.getFileToPath() + "] in input row";
        logError(message);
        throw new KettleStepException(message);
      }
    }

    if (meta.getTargetWidth() != null && data.indexOfTargetWidthField < 0) {
      data.indexOfTargetWidthField = getInputRowMeta().indexOfValue(meta.getTargetWidth());
      if (data.indexOfTargetWidthField < 0) {
        final String message =
            "Unable to find table name field [" + meta.getTargetWidth() + "] in input row";
        logError(message);
        throw new KettleStepException(message);
      }
    }

    if (meta.getTargetHeight() != null && data.indexOfTargetHeightField < 0) {
      data.indexOfTargetHeightField = getInputRowMeta().indexOfValue(meta.getTargetHeight());
      if (data.indexOfTargetHeightField < 0) {
        final String message =
            "Unable to find table name field [" + meta.getTargetHeight() + "] in input row";
        logError(message);
        throw new KettleStepException(message);
      }
    }
  }
}
