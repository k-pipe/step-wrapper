USER=kpipe
IMAGE_NAME=step-wrapper
IMAGE_TAG=$1
IMAGE=$USER/$IMAGE_NAME:$IMAGE_TAG
echo "===================================================================="
echo "  Building docker image $IMAGE"
echo "===================================================================="
docker build . -t $IMAGE
