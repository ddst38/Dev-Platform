FROM debian:bookworm-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        cvs \
        openbsd-inetd \
        openssh-client \
    && rm -rf /var/lib/apt/lists/*
# Déclarer le service CVS pour inetd
RUN echo "cvspserver 2401/tcp" >> /etc/services
ENV CVSROOT=/cvsroot

# Configuration CVS pserver pour inetd
RUN echo "cvspserver stream tcp nowait root /usr/bin/cvs cvs -f --allow-root=/cvsroot pserver" \
    > /etc/inetd.conf

EXPOSE 2401

CMD sh -c "\
  if [ ! -d /cvsroot/CVSROOT ]; then \
    cvs -d /cvsroot init; \
  fi && \
  inetd -d"