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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.io.Files;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.databinding.FragmentGeneralMessageBinding;
import edu.mit.ll.nics.android.interfaces.DestinationResponse;
import edu.mit.ll.nics.android.interfaces.WorkerCallback;
import edu.mit.ll.nics.android.repository.GeneralMessageRepository;
import edu.mit.ll.nics.android.repository.NetworkRepository;
import edu.mit.ll.nics.android.ui.fragments.AppFragment;
import edu.mit.ll.nics.android.ui.viewmodel.GeneralMessageViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.GeneralMessageViewModel.GeneralMessageViewModelFactory;
import edu.mit.ll.nics.android.utils.CheckPermissions;
import edu.mit.ll.nics.android.utils.ImageUtils;
import edu.mit.ll.nics.android.workers.DownloadImageWorker;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.BitmapUtils.getScaledBitmap;
import static edu.mit.ll.nics.android.utils.BitmapUtils.scaleBitmap;
import static edu.mit.ll.nics.android.utils.CheckPermissions.getFilePermissions;
import static edu.mit.ll.nics.android.utils.FileUtils.createJpegFile;
import static edu.mit.ll.nics.android.utils.FileUtils.getFileNameFromUri;
import static edu.mit.ll.nics.android.utils.GeoUtils.getLocationFromString;
import static edu.mit.ll.nics.android.utils.ImageUtils.getRealPathFromURI;
import static edu.mit.ll.nics.android.utils.ImageUtils.getTaggedRotationMatrix;
import static edu.mit.ll.nics.android.utils.ImageUtils.saveImageToDevice;
import static edu.mit.ll.nics.android.utils.ImageUtils.saveImageToStorage;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;
import static edu.mit.ll.nics.android.utils.Utils.hasNoValue;
import static edu.mit.ll.nics.android.utils.Utils.isTrue;
import static edu.mit.ll.nics.android.utils.Utils.navigateSafe;
import static edu.mit.ll.nics.android.utils.Utils.popBackStack;
import static edu.mit.ll.nics.android.utils.Utils.removeSafe;
import static edu.mit.ll.nics.android.utils.Utils.showSimpleDialog;
import static edu.mit.ll.nics.android.utils.constants.Intents.PICK_LOCATION_REQUEST;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.NICS.MAX_POST_IMAGE_QUALITY;
import static edu.mit.ll.nics.android.utils.constants.NICS.MAX_POST_IMAGE_SIZE;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_FILE_PROVIDER;

@AndroidEntryPoint
public class GeneralMessageFragment extends AppFragment {

    private GeneralMessage mReport;
    private GeneralMessageViewModel mViewModel;
    private FragmentGeneralMessageBinding mBinding;
    private ActivityResultLauncher<Uri> mTakePicture;
    private ActivityResultLauncher<String> mBrowseGallery;
    private ActivityResultLauncher<String> mRequestCameraPermission;
    private ActivityResultLauncher<String[]> mRequestFilePermissions;
    private static final int MAX_PREVIEW_SIZE = 800;

    // TODO work out the form change between different types of coordinate systems.

    @Inject
    GeneralMessageViewModelFactory mViewModelFactory;

    @Inject
    GeneralMessageRepository mRepository;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register on back pressed callback.
        mActivity.getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

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
     * nics/src/main/res/layout/fragment_general_message.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_general_message, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link GeneralMessageViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();

        // Get the navigation controller for this view to use for navigating between the panels.
        mNavController = Navigation.findNavController(requireView());

        mReport = initGeneralMessage();
        Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_photo_white);

        // Create a new instance of the view model for this fragment.
        GeneralMessageViewModel.Factory factory = new GeneralMessageViewModel.Factory(mViewModelFactory, mReport, defaultBitmap);
        mViewModel = new ViewModelProvider(this, factory).get(GeneralMessageViewModel.class);

        // Bind all variables to the xml.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);
        mBinding.setFragment(this);

        subscribeToModel();

        // If the report has an image, download it and load it into the image view.
        downloadImage(mReport.getFullPath());
    }

    /**
     * Subscribe this fragment to the {@link GeneralMessageViewModel} to observe changes and dynamically
     * update the UI components and vice versa.
     */
    private void subscribeToModel() {
        // Listen for responses from the map point selector dialog.
        subscribeToDestinationResponse(R.id.generalMessageFragment, PICK_LOCATION_REQUEST, (DestinationResponse<LatLng>) location -> {
            if (location != null) {
                mViewModel.setLatitude(String.valueOf(location.latitude));
                mViewModel.setLongitude(String.valueOf(location.longitude));
            }
            removeSafe(mNavBackStackEntry.getSavedStateHandle(), PICK_LOCATION_REQUEST);
        });

        mViewModel.getDescription().observe(mLifecycleOwner, description -> {
            mReport.setDescription(description);
            mBinding.executePendingBindings();
        });

        mViewModel.getLatitude().observe(mLifecycleOwner, latitude -> {
            mReport.setLatitude(getLocationFromString(latitude));
            mBinding.executePendingBindings();
        });

        mViewModel.getLongitude().observe(mLifecycleOwner, longitude -> {
            mReport.setLongitude(getLocationFromString(longitude));
            mBinding.executePendingBindings();
        });

        mViewModel.isDraft().observe(mLifecycleOwner, isDraft -> {
            mReport.setDraft(isDraft);
            onBackPressedCallback.setEnabled(isDraft);
            mBinding.executePendingBindings();
        });

        mViewModel.getCoordinateRepresentation().observe(mLifecycleOwner, representation -> {
            // TODO switch inputs might not even need this
        });
    }

    /**
     * Unbind from all xml layouts and cancel any pending dialogs.
     */
    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        removeSafe(mNavBackStackEntry.getSavedStateHandle(), PICK_LOCATION_REQUEST);
        onBackPressedCallback.remove();
        super.onDestroy();
    }

    /**
     * If the report is a draft report, we want to show a warning to the user to confirm that they
     * will be exiting without saving.
     */
    final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {
            showExitWithoutSavingDialog();
        }
    };

    /**
     * Initialize the general message that will be tied to this form.
     * Either create a new one, or load one from the database using the id.
     *
     * @return {@link GeneralMessage} The general message that will be associated with this form.
     */
    private GeneralMessage initGeneralMessage() {
        long id = GeneralMessageFragmentArgs.fromBundle(getArguments()).getId();

        if (id != -1L) {
            return mRepository.getGeneralMessageById(id);
        } else {
            return new GeneralMessage().create(mPreferences);
        }
    }

    /**
     * On click callback for the submit button. If either the latitude or longitude inputs don't
     * have values, then an alert dialog appears to warn the user that there is no location set for
     * the form. They can either submit the form as is, or go back and add a location.
     */
    public void submitReport() {
        if (hasNoValue(mReport.getLatitude()) || hasNoValue(mReport.getLongitude())) {
            new MaterialAlertDialogBuilder(mActivity)
                    .setTitle("No Location Set.")
                    .setIcon(R.drawable.nics_logo)
                    .setMessage("A location has not been set for this report. Would you like to set a location before sending?")
                    .setPositiveButton(getString(R.string.yes), (dialog, which) -> dialog.dismiss())
                    .setNegativeButton(getString(R.string.no), (dialog, which) -> submit())
                    .setCancelable(false)
                    .create()
                    .show();
        } else {
            submit();
        }
    }

    /**
     * Adds the {@link GeneralMessage} form to the database and posts it to the server using the
     * {@link NetworkRepository#postGeneralMessages()} api call. It then pops the current fragment off of
     * the back stack so that the view model can be properly cleared.
     */
    private void submit() {
        if (mViewModel.getBitmap().getValue() != null) {
            //scale bitmap to max 1024 in either direction and lower quality to 80%
            Bitmap bitmap = scaleBitmap(mViewModel.getBitmap().getValue(), MAX_POST_IMAGE_SIZE);
            File temp = createJpegFile(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            Uri uri = saveImageToDevice(bitmap, temp, MAX_POST_IMAGE_QUALITY);

            saveImageToStorage(bitmap, mContext.getContentResolver(), MAX_POST_IMAGE_QUALITY);
            mReport.setFullPath(uri.getPath());
        }

        mReport.setSeqTime(System.currentTimeMillis());
        mReport.setDraft(false);

        mRepository.addGeneralMessageToDatabase(mReport, result -> mMainHandler.post(() -> {
            mNetworkRepository.postGeneralMessages();
            popBackStack(mNavController);
        }));
    }

    /**
     * On click callback for the clear form button. Shows an alert dialog confirming to the user that
     * they want to clear the form. If they say turnOn, it will clear all of the form data.
     */
    public void showClearDialog() {
        new MaterialAlertDialogBuilder(mActivity)
                .setTitle("Clear Form")
                .setIcon(R.drawable.nics_logo)
                .setMessage("Would you like to clear this form?")
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> clearForm())
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .create()
                .show();
    }

    private void clearForm() {
        mViewModel.setPhotoUri(null);
        mViewModel.setCurrentImageRotation(0);
        mViewModel.setDescription(EMPTY);
        mViewModel.setLatitude("0.0");
        mViewModel.setLongitude("0.0");
        mViewModel.setDraft(true);
        mViewModel.setBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_photo_white));
        mReport.setFullPath(EMPTY);
    }

    /**
     * If the user is editing a draft form and clicks the back button, this will show an
     * AlertDialog verifying that the user wants to exit without saving the draft or
     * submitting.
     */
    public void showExitWithoutSavingDialog() {
        new MaterialAlertDialogBuilder(mActivity)
                .setTitle(getString(R.string.confirm_continue_to_title))
                .setIcon(R.drawable.nics_logo)
                .setMessage(String.format(getString(R.string.confirm_continue_to_description), getString(R.string.GENERALMESSAGE)))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> popBackStack(mNavController))
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .create()
                .show();
    }

    /**
     * Saves the form as it currently is to the local database and leaves it as a draft, so that
     * the user can continue working on it later. It then pops the current fragment off of the back
     * stack so that the view model can be properly cleared.
     */
    public void saveAsDraft() {
        mReport.setSeqTime(System.currentTimeMillis());
        mRepository.addGeneralMessageToDatabase(mReport);
        popBackStack(mNavController);
    }

    /**
     * Copy the current report as a new draft report.
     */
    public void copyAsNewReport() {
        mViewModel.setDraft(true);
        mReport.setId(-1L);
    }

    /**
     * Rotate the image bitmap left by 90 degrees.
     */
    public void rotateImageLeft() {
        Bitmap bitmap = mViewModel.getBitmap().getValue();
        if (mViewModel.getBitmap() != null) {
            mViewModel.setBitmap(ImageUtils.rotateBitmap(bitmap, 90));
            mViewModel.setCurrentImageRotation(mViewModel.getCurrentImageRotation() + 90);
        }
    }

    /**
     * Rotate the image bitmap right by 90 degrees.
     */
    public void rotateImageRight() {
        Bitmap bitmap = mViewModel.getBitmap().getValue();
        if (mViewModel.getBitmap() != null) {
            mViewModel.setBitmap(ImageUtils.rotateBitmap(bitmap, -90));
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
        try (InputStream stream = mContext.getContentResolver().openInputStream(uri)){
            // Get a file inputstream from the uri.
            String fileName = getFileNameFromUri(mContext.getContentResolver(), uri);

            // Save the inputstream data to a new temp file.
            File temp = File.createTempFile(fileName, ".jpg", mActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            String path = temp.getAbsolutePath();

            Files.asByteSink(temp).writeFrom(stream);

            Bitmap image = getScaledBitmap(path, MAX_PREVIEW_SIZE, MAX_PREVIEW_SIZE);
            Matrix orientationMatrix = getTaggedRotationMatrix(path);
            image = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), orientationMatrix, true);

            mReport.setFullPath(path);
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
            // Ensure that there's a camera activity to handle the intent.
            if (takePictureIntent.resolveActivity(mActivity.getPackageManager()) != null) {
                // Create the File where the photo should go.
                File photoFile = createJpegFile(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES));

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
                mReport.setFullPath(path);
                mViewModel.setBitmap(image);
                mViewModel.setPhotoUri(null);
            } catch (IllegalArgumentException e) {
                Timber.tag(DEBUG).w(e, "Failed to get scaled bitmap from image capture.");
            }
        }
    }

    // TODO can probably have a more elegant way of doing this with binding adapter, etc. but doing this for now.

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

    public void setMyLocation() {
        if (Double.isNaN(mPreferences.getMDTLatitude()) || Double.isNaN(mPreferences.getMDTLongitude())) {
            Snackbar.make(requireView(), getString(R.string.cant_find_your_location), Snackbar.LENGTH_LONG).show();
        } else {
            mViewModel.setLatitude(String.valueOf(mPreferences.getMDTLatitude()));
            mViewModel.setLongitude(String.valueOf(mPreferences.getMDTLongitude()));
        }
    }

    public void openInMap() {
        LatLng latLng = null;

        try {
            String latitude = mViewModel.getLatitude().getValue();
            String longitude = mViewModel.getLongitude().getValue();

            latLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
        } catch (Exception ignored) {
        }

        GeneralMessageFragmentDirections.OpenLocationSelector action = GeneralMessageFragmentDirections.openLocationSelector();
        action.setSelectionMode(true);
        action.setSelectionPoint(latLng);
        navigateSafe(mNavController, action);
    }
}
