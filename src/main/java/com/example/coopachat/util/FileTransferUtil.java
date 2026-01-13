package com.example.coopachat.util;

import com.jcraft.jsch.*;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FileTransferUtil {

    private static final String FILE_UPLOAD_DIRECTORY = "./files/";
    private static final String REMOTE_DIR = "/coopachat/";
    private static final String SFTP_HOST = "185.170.213.160";
    private static final int SFTP_PORT = 22;
    private static final String SFTP_USER = "root";
    private static final String SFTP_PASSWORD = "GafHcHPAApykVYje#";

    /**
     * Upload a single file locally and transfer it to remote server.
     */
    public static String handleFileUpload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return "";

        String fileName = generateUniqueFileName(file.getOriginalFilename());
        File uploadDir = new File(FILE_UPLOAD_DIRECTORY);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        Path localFilePath = Paths.get(FILE_UPLOAD_DIRECTORY, fileName);
        Files.copy(file.getInputStream(), localFilePath, StandardCopyOption.REPLACE_EXISTING);

        transferFileToRemote(localFilePath.toString(), REMOTE_DIR + fileName);
        return fileName;
    }

    /**
     * Upload multiple pictures.
     */
    public static List<String> uploadPictures(List<MultipartFile> pictures) throws IOException {
        List<String> pictureUrls = new ArrayList<>();
        if (pictures == null || pictures.isEmpty()) return pictureUrls;

        for (MultipartFile picture : pictures) {
            if (picture != null && !picture.isEmpty()) {
                String fileName = handleFileUpload(picture);
                if (!fileName.isEmpty()) {
                    pictureUrls.add(fileName);
                }
            }
        }
        return pictureUrls;
    }

    /**
     * Generate unique file name with original extension.
     */
    public static String generateUniqueFileName(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        return UUID.randomUUID().toString() + (extension != null ? "." + extension : "");
    }

    /**
     * Transfer file to remote server using SFTP.
     */
    public static void transferFileToRemote(String localFilePath, String remoteFilePath) {
        try (SftpClient sftp = new SftpClient()) {
            sftp.uploadFile(localFilePath, remoteFilePath);
        } catch (Exception e) {
            System.err.println("[ERREUR] Transfert échoué : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Delete a remote file using SFTP.
     */
    public static void deleteRemoteFile(String remoteFilePath) {
        try (SftpClient sftp = new SftpClient()) {
            sftp.deleteFile(remoteFilePath);
            System.out.println("[INFO] Fichier supprimé : " + remoteFilePath);
        } catch (Exception e) {
            System.err.println("[ERREUR] Suppression échouée : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Internal SFTP Client to avoid code repetition.
     */
    private static class SftpClient implements AutoCloseable {
        private final Session session;
        private final ChannelSftp sftpChannel;

        public SftpClient() throws JSchException {
            JSch jsch = new JSch();
            session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            sftpChannel = (ChannelSftp) channel;
        }

        public void uploadFile(String localFilePath, String remoteFilePath) throws SftpException, IOException {
            try (InputStream inputStream = new FileInputStream(localFilePath)) {
                sftpChannel.put(inputStream, remoteFilePath);
            }
        }

        public void deleteFile(String remoteFilePath) throws SftpException {
            sftpChannel.rm(remoteFilePath);
        }

        @Override
        public void close() {
            if (sftpChannel != null && sftpChannel.isConnected())
                sftpChannel.disconnect();
            if (session != null && session.isConnected())
                session.disconnect();
        }
    }
}
