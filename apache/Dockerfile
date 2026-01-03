FROM httpd:2.4

RUN apt-get update && apt-get install -y python3 python3-pip \
 && pip3 install --break-system-packages legacy-cgi
# Enable CGI
RUN sed -i \
    -e 's/^#LoadModule cgi_module/LoadModule cgi_module/' \
    -e 's/^#LoadModule alias_module/LoadModule alias_module/' \
    /usr/local/apache2/conf/httpd.conf

# Copy config
COPY httpd.conf /usr/local/apache2/conf/httpd.conf

# Upload script
COPY upload.py /usr/lib/cgi-bin/upload.py
RUN chmod +x /usr/lib/cgi-bin/upload.py

# Create reports directory
RUN mkdir -p /var/www/reports \
 && chown -R www-data:www-data /var/www/reports