package com.example.infovista.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    
    /**
     * Get a real file from a content URI by copying it to cache
     */
    public static File getFileFromUri(Context context, Uri uri) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            
            // Get the file name
            String fileName = getFileName(contentResolver, uri);
            if (fileName == null) {
                fileName = "temp_" + System.currentTimeMillis();
            }
            
            // Get file extension
            String extension = getFileExtension(contentResolver, uri);
            if (extension != null && !fileName.contains(".")) {
                fileName = fileName + "." + extension;
            }
            
            // Create temp file in cache directory
            File cacheDir = context.getCacheDir();
            File file = new File(cacheDir, fileName);
            
            // Copy content to file
            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null) {
                return null;
            }
            
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            outputStream.close();
            inputStream.close();
            
            return file;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get file name from URI
     */
    private static String getFileName(ContentResolver contentResolver, Uri uri) {
        String fileName = null;
        String[] projection = {OpenableColumns.DISPLAY_NAME};
        
        try (Cursor cursor = contentResolver.query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        }
        
        return fileName;
    }
    
    /**
     * Get file extension from URI
     */
    private static String getFileExtension(ContentResolver contentResolver, Uri uri) {
        String mimeType = contentResolver.getType(uri);
        if (mimeType != null) {
            return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        }
        return null;
    }
}
