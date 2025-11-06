# To learn more about how to use Nix to configure your environment
# see: https://firebase.google.com/docs/studio/customize-workspace
{ ... }: {
  # Which nixpkgs channel to use.
  channel = "stable-24.05"; # or "unstable"

  # Use https://search.nixos.org/packages to find packages
  packages = [
    # pkgs.go
    # pkgs.python311
    # pkgs.python311Packages.pip
    # pkgs.nodejs_20
    # pkgs.nodePackages.nodemon
  ];


  # Sets environment variables in the workspace
  env = {
  GEMINI_API_KEY = "AIzaSyC0ew0UaA5fZ6rhVtIVGQuqHhQItiUFFJk"; 
};

  idx = {
    # Search for the extensions you want on https://open-vsx.org/ and use "publisher.id"
    extensions = [
      # "vscodevim.vim"
    ];

    # Enable previews
    previews = {
      enable = true;
      previews = {
        # web = {
        #   command = ["npm", "run", "dev", "--", "--port", "$PORT", "--host", "0.0.0.0"];
        #   manager = "web";
        #   env = {
        #     # Environment variables for the preview command
        #   };
        # };
      };
    };
  };
}
