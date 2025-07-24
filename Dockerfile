FROM jenkins/jenkins:lts

USER root

# Install dependencies
RUN apt-get update && \
    apt-get install -y docker.io curl unzip && \
    usermod -aG docker jenkins

# Install OpenShift CLI (oc)
RUN curl -L https://mirror.openshift.com/pub/openshift-v4/clients/ocp/latest/openshift-client-linux.tar.gz -o oc.tar.gz && \
    tar -xzf oc.tar.gz -C /usr/local/bin oc && \
    rm oc.tar.gz

USER jenkins