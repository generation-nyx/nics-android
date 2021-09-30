/*
 * Copyright (c) 2008-2021, Massachusetts Institute of Technology (MIT)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.mit.ll.nics.android.ui.fragments.reports;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;

import com.google.android.material.snackbar.Snackbar;
import com.google.common.io.Files;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.databinding.FragmentEodImageBinding;
import edu.mit.ll.nics.android.interfaces.WorkerCallback;
import edu.mit.ll.nics.android.ui.fragments.TabFragment;
import edu.mit.ll.nics.android.ui.viewmodel.EODReportViewModel;
import edu.mit.ll.nics.android.utils.CheckPermissions;
import edu.mit.ll.nics.android.utils.ImageUtils;
import edu.mit.ll.nics.android.workers.DownloadImageWorker;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.BitmapUtils.getScaledBitmap;
import static edu.mit.ll.nics.android.utils.CheckPermissions.getFilePermissions;
import static edu.mit.ll.nics.android.utils.FileUtils.createJpegFile;
import static edu.mit.ll.nics.android.utils.FileUtils.getFileNameFromUri;
import static edu.mit.ll.nics.android.utils.ImageUtils.getRealPathFromURI;
import static edu.mit.ll.nics.android.utils.ImageUtils.getTaggedRotationMatrix;
import static edu.mit.ll.nics.android.utils.ImageUtils.rotateBitmap;
import static edu.mit.ll.nics.android.utils.ImageUtils.saveImageToStorage;
import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;
import static edu.mit.ll.nics.android.utils.Utils.isTrue;
import static edu.mit.ll.nics.android.utils.Utils.showSimpleDialog;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_FILE_PROVIDER;

@AndroidEntryPoint
public class EODImageFragment extends TabFragment {

    private EODReportViewModel mViewModel;
    private FragmentEodImageBinding mBinding;
    private ActivityResultLauncher<Uri> mTakePicture;
    private ActivityResultLauncher<String> mBrowseGallery;
    private ActivityResultLauncher<String> mRequestCameraPermission;
    private ActivityResultLauncher<String[]> mRequestFilePermissions;
    private static final int MAX_PREVIEW_SIZE = 800;

    public static EODImageFragment newInstance() {
        return new EODImageFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register for activity result callback for when the user responds to the camera permission dialog.
        mRequestCameraPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), this::onRequestCameraPermissionResult);

        // Register for activity result callback for when the user responds to the file permissions request.
        mRequestFilePermissions = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onRequestFilesPermissionResult);

        // Register for activity result callback for when the user takes a picture.
        mTakePicture = registerForActivityResult(new ActivityResultContracts.TakePicture(), this::onTakePictureResult);

        // Register for activity result callback for when the user selects an image from the gallery.
        mBrowseGallery = registerForActivityResult(new ActivityResultContracts.GetContent(), this::onBrowseImageGalleryResult);
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_eod_image.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_eod_image, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the shared {@link EODReportViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();

        // Get the EOD shared view model.
        mViewModel = new ViewModelProvider(requireParentFragment()).get(EODReportViewModel.class);

        // Bind all variables to the xml.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);
        mBinding.setFragment(this);

        downloadImage(mViewModel.getEODReport().getFullPath());
    }

    /**
     * Unbind from all xml layouts and cancel any pending dialogs.
     */
    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    /**
     * Rotate the image bitmap left by 90 degrees.
     */
    public void rotateImageLeft() {
        Bitmap bitmap = mViewModel.getBitmap().getValue();
        if (mViewModel.getBitmap() != null) {
            mViewModel.setBitmap(rotateBitmap(bitmap, 90));
            mViewModel.setCurrentImageRotation(mViewModel.getCurrentImageRotation() + 90);
        }
    }

    /**
     * Rotate the image bitmap right by 90 degrees.
     */
    public void rotateImageRight() {
        Bitmap bitmap = mViewModel.getBitmap().getValue();
        if (mViewModel.getBitmap() != null) {
            mViewModel.setBitmap(rotateBitmap(bitmap, -90));
            mViewModel.setCurrentImageRotation(mViewModel.getCurrentImageRotation() - 90);
        }
    }

    /**
     * Called when the request for the user's permission for camera access is finished. If they
     * granted access, then we can open the camera. If not, show them a message telling them why
     * they can't open the camera.
     * @param isGranted Whether or not the user granted access to use the camera.
     */
    private void onRequestCameraPermissionResult(boolean isGranted) {
        if (isGranted) {
            takePicture();
        } else {
            Snackbar.make(requireView(), "You can't use the camera without accepting the camera permission.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void onRequestFilesPermissionResult(Map<String, Boolean> results) {
        if (isTrue(results.values())) {
            Timber.tag(DEBUG).i("File permissions accepted.");
            browseImageGallery();
        } else {
            Timber.tag(DEBUG).i("File permissions denied.");
            Snackbar.make(requireView(), "You can't browse for files without accepting the files permission.", Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * On click callback for the the browse gallery button. Starts the
     * {@link ActivityResultContracts.GetContent} launcher that select an image from their device.
     */
    public void browseImageGallery() {
        if (CheckPermissions.hasFilePermissions(mContext)) {
            mBrowseGallery.launch("image/*");
        } else {
            mRequestFilePermissions.launch(getFilePermissions());
        }
    }

    /**
     * The result callback from the {@link ActivityResultContracts.GetContent} call that is used to
     * browse for image from the user's image gallery. Creates a copy of the selected image and
     * saves it to a temp file. Then creates a scaled bitmap and saves that to the view model.
     */
    private void onBrowseImageGalleryResult(Uri uri) {
        try (InputStream stream = mContext.getContentResolver().openInputStream(uri)) {
            // Get a file inputstream from the uri.
            String fileName = getFileNameFromUri(mContext.getContentResolver(), uri);

            // Save the inputstream data to a new temp file.
            File temp = File.createTempFile(fileName, ".jpg", mActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            String path = temp.getAbsolutePath();

            Files.asByteSink(temp).writeFrom(stream);

            Bitmap image = getScaledBitmap(path, MAX_PREVIEW_SIZE, MAX_PREVIEW_SIZE);
            Matrix orientationMatrix = getTaggedRotationMatrix(path);
            image = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), orientationMatrix, true);

            mViewModel.getEODReport().setFullPath(path);
            mViewModel.setBitmap(image);
        } catch (Exception e) {
            showSimpleDialog(mActivity, getString(R.string.selected_image_not_available_title), getString(R.string.selected_image_not_available_desc));
            Timber.tag(DEBUG).e(e, "Failed to select an image from the device.");
        }
    }

    /**
     * On click callback for the the capture image button. Creates a file for the image to be saved
     * to after it's captured and start the {@link MediaStore#ACTION_IMAGE_CAPTURE} intent using
     * the {@link ActivityResultContracts.TakePicture} launcher that allows the user to take a
     * picture with their camera and save it to that file location.
     */
    public void takePicture() {
        // Verify that the user has granted the camera permission. If not request it from them.
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(mActivity.getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = createJpegFile(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES));

                // Continue only if the File was successfully created
                mViewModel.setPhotoUri(Uri.fromFile(photoFile));
                Uri photoURI = FileProvider.getUriForFile(mActivity, NICS_FILE_PROVIDER, photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                mTakePicture.launch(photoURI);
            }
        } else {
            mRequestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * The result callback from the {@link ActivityResultContracts.GetContent} call that is used to
     * browse for image from the user's image gallery. Creates a copy of the selected image and
     * saves it to a temp file. Then creates a scaled bitmap and saves that to the view model.
     *
     * @param success Whether or not taking the picture was successful.
     */
    private void onTakePictureResult(boolean success) {
        Uri photoUri = mViewModel.getPhotoUri();
        if (success && mViewModel != null && photoUri != null) {
            try {
                String path = getRealPathFromURI(photoUri, mContext);
                Bitmap image = getScaledBitmap(path, MAX_PREVIEW_SIZE, MAX_PREVIEW_SIZE);
                Matrix orientationMatrix = getTaggedRotationMatrix(photoUri, mContext);
                image = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), orientationMatrix, true);

                // TODO saving the image to the gallery, but still using the path from the original image. For scoped storage, it's a bit more complicated.
                saveImageToStorage(image, mContext.getContentResolver());
                mViewModel.getEODReport().setFullPath(path);
                mViewModel.setBitmap(image);
                mViewModel.setPhotoUri(null);
            } catch (IllegalArgumentException e) {
                Timber.tag(DEBUG).w(e, "Failed to get scaled bitmap from image capture.");
            }
        }
    }

    /**
     * Start the {@link DownloadImageWorker} to try and download the image from the url path. It will
     * either be a remote path or a local path.
     *
     * @param imagePath The image path to use to download the image.
     */
    private void downloadImage(String imagePath) {
        if (!emptyCheck(imagePath)) {
            OneTimeWorkRequest request = mNetworkRepository.downloadImage(imagePath);

            subscribeToWorker(request, new WorkerCallback() {
                @Override
                public void onSuccess(@NotNull WorkInfo workInfo) {
                    String result = workInfo.getOutputData().getString("uri");

                    if (!emptyCheck(result)) {
                        try {
                            Uri uri = Uri.parse(result);
                            mViewModel.setBitmap(ImageUtils.decodeScaledImage(mBinding.formImageSelectorPreview, uri.getPath()));
                        } catch (Exception e) {
                            Timber.tag(DEBUG).e(e, "Failed to load bitmap from uri.");
                        }
                    }

                    mViewModel.setLoading(false);
                }

                @Override
                public void onFailure(@NotNull WorkInfo workInfo) {
                    mViewModel.setLoading(false);
                }

                @Override
                public void onWorking() {
                    mViewModel.setLoading(true);
                }
            });
        }
    }

    @Override
    public void refresh() {

    }

    @Override
    public String getTabTitle(Context context) {
        return context.getString(R.string.eod_image_section_displayname);
    }

    @Override
    public String getTabContentDescription(Context context) {
        return context.getString(R.string.eod_image_section_displayname);
    }
}
