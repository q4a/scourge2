package org.scourge;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.resource.ResourceLocator;
import com.ardor3d.util.resource.ResourceSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

/**
 * User: gabor
 * Date: 1/4/11
 * Time: 9:49 AM
 */
public class FileResourceLocator implements ResourceLocator {
    private final String _baseDir;

    public FileResourceLocator(final String baseDir) throws URISyntaxException {
        if (baseDir == null) {
            throw new NullPointerException("baseDir can not be null.");
        }
        _baseDir = baseDir.endsWith(File.separator) ? baseDir : baseDir + File.separator;
    }

    public ResourceSource locateResource(final String resourceName) {
        return doRecursiveLocate(resourceName);
    }

    protected ResourceSource doRecursiveLocate(String resourceName) {
        // Trim off any prepended local dir.
        while (resourceName.startsWith("./") && resourceName.length() > 2) {
            resourceName = resourceName.substring(2);
        }
        while (resourceName.startsWith(".\\") && resourceName.length() > 2) {
            resourceName = resourceName.substring(2);
        }

        // Try to locate using resourceName as is.
        File base = new File(_baseDir);
        File rVal;
        while(base != null) {
            rVal = new File(base, resourceName);
            if(rVal.exists()) {
                return new FileResourceSource(rVal);
            }
            base = base.getParentFile();
        }
        return null;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof FileResourceLocator &&
               _baseDir.equals(((FileResourceLocator) obj)._baseDir);
    }

    class FileResourceSource implements ResourceSource {
        private File file;
        private String type;

        public FileResourceSource(File file) {
            this.file = file;
            String fileName = file.getName();
            final int dot = fileName.lastIndexOf('.');
            if (dot >= 0) {
                type = fileName.substring(dot);
            } else {
                type = UNKNOWN_TYPE;
            }
        }

        @Override
        public String getName() {
            return file.getPath();
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public ResourceSource getRelativeSource(String s) {
            throw new RuntimeException("Implement me");
        }

        @Override
        public InputStream openStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public void write(OutputCapsule outputCapsule) throws IOException {
            throw new RuntimeException("Implement me");
        }

        @Override
        public void read(InputCapsule inputCapsule) throws IOException {
            throw new RuntimeException("Implement me");
        }

        @Override
        public Class<?> getClassTag() {
            return getClass();
        }

        /**
         * @return the string representation of this URLResourceSource.
         */
        @Override
        public String toString() {
            return "FileResourceSource [file=" + file.getPath() + ", type=" + type + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            result = prime * result + ((file == null) ? 0 : file.getPath().hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof FileResourceSource)) {
                return false;
            }
            final FileResourceSource other = (FileResourceSource) obj;
            if (type == null) {
                if (other.type != null) {
                    return false;
                }
            } else if (!type.equals(other.type)) {
                return false;
            }
            if (file == null) {
                if (other.file != null) {
                    return false;
                }
            } else if (!file.getPath().equals(other.file.getPath())) {
                return false;
            }
            return true;
        }
    }
}
