FROM tomcat:10-jdk21-openjdk

WORKDIR /usr/local/tomcat
# Création des répertoires pour le stockage local
RUN mkdir -p /usr/local/tomcat/uploads
RUN mkdir -p /usr/local/tomcat/logs

# Copie l'application
COPY target/LegoProject-1.0.war webapps/

EXPOSE 8080

#  les variables d'environnement avec valeurs par défaut
ENV CACHE_ENABLED=true
ENV MONGODB_URI=mongodb://root:lego@mongo-service:27017/legodb?authSource=admin
ENV REDIS_HOST=redis-service
ENV REDIS_PORT=6379
ENV BLOB_STORAGE_TYPE=local
ENV FILE_STORAGE_PATH=/usr/local/tomcat/uploads
ENV UPLOAD_DIR=/usr/local/tomcat/uploads

CMD ["catalina.sh", "run"]