import UploadButton from "./UploadButton";

function Dashboard() {
  return (
    <div className="w-full min-h-[90vh] bg-gradient-to-r from-red-100 via-white to-red-100 flex items-center justify-center px-6">
      <div className="max-w-screen-xl w-full flex flex-col md:flex-row items-center justify-between gap-10 py-10 md:py-20">

        <div className="flex-1 flex flex-col justify-center items-start gap-5 text-center md:text-left">
          <h1 className="text-4xl md:text-6xl font-extrabold text-gray-800 leading-tight">
            Rule<span className="text-red-600">X</span> Engine
          </h1>
          <p className="text-lg md:text-2xl max-w-xl text-gray-700">
            Upload your Excel and let the app automatically clean and validate your data
            — <span className="text-red-600 font-semibold">faster, easier, and zero manual checks.</span>
            <br />
            <span className="text-base md:text-lg text-gray-600 mt-2 block">
              Choose between <span className="font-semibold">Validation Mode</span> to find issues or <span className="font-semibold text-green-600">Auto Clean Mode</span> to automatically fix them!
            </span>
          </p>
          <UploadButton link='/upload' text='Upload Excel' />
        </div>

        <div className="flex-1 flex justify-center items-center">
          <img
            src="/image.png"
            alt="Excel Illustration"
            className="max-w-md md:max-w-xl w-full"
          />
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
