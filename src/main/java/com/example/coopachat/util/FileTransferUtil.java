package com.example.coopachat.util;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Component
public class FileTransferUtil {

    private static final Logger log = LoggerFactory.getLogger(FileTransferUtil.class);

    @Value("${app.files.dir:files}")
    private String fileUploadDirectory;

    @Value("${sftp.enabled:true}")
    private boolean sftpEnabled;

    @Value("${sftp.host:}")
    private String sftpHost;

    @Value("${sftp.port:22}")
    private int sftpPort;

    @Value("${sftp.user:}")
    private String sftpUser;

    @Value("${sftp.password:}")
    private String sftpPassword;

    @Value("${sftp.remote.dir:/coopachat/}")
    private String remoteDir;

    /**
     * Upload a single file locally and transfer it to remote server.
     */
    public String handleFileUpload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return "";

        String fileName = generateUniqueFileName(file.getOriginalFilename());
        Path uploadDir = Paths.get(fileUploadDirectory).toAbsolutePath().normalize();
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Path localFilePath = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), localFilePath, StandardCopyOption.REPLACE_EXISTING);

        if (sftpEnabled) {
            transferFileToRemote(localFilePath.toString(), remoteDir + fileName);
        }
        return fileName;
    }

    /**
     * Upload multiple pictures.
     */
    public List<String> uploadPictures(List<MultipartFile> pictures) throws IOException {
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
    private static String generateUniqueFileName(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        return UUID.randomUUID().toString() + (extension != null ? "." + extension : "");
    }

    /**
     * Transfer file to remote server using SFTP.
     */
    public void transferFileToRemote(String localFilePath, String remoteFilePath) {
        if (!sftpEnabled) {
            return;
        }
        try (SftpClient sftp = new SftpClient(sftpHost, sftpPort, sftpUser, sftpPassword)) {
            sftp.uploadFile(localFilePath, remoteFilePath);
        } catch (Exception e) {
            log.warn("Transfert SFTP échoué: {}", e.getMessage());
        }
    }

    /**
     * Delete a remote file using SFTP.
     */
    public void deleteRemoteFile(String remoteFilePath) {
        if (!sftpEnabled) {
            return;
        }
        try (SftpClient sftp = new SftpClient(sftpHost, sftpPort, sftpUser, sftpPassword)) {
            sftp.deleteFile(remoteFilePath);
            log.info("Fichier supprimé (SFTP): {}", remoteFilePath);
        } catch (Exception e) {
            log.warn("Suppression SFTP échouée: {}", e.getMessage());
        }
    }

    /**
     * Internal SFTP Client to avoid code repetition.
     */
    private static class SftpClient implements AutoCloseable {
        private final Session session;
        private final ChannelSftp sftpChannel;

        public SftpClient(String host, int port, String user, String password) throws JSchException {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, port);
            session.setPassword(password);

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
